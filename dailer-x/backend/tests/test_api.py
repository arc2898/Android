from app.main import app, init_db
from fastapi.testclient import TestClient

client = TestClient(app)
init_db()


def test_health():
    response = client.get('/health')
    assert response.status_code == 200
    assert response.json()['status'] == 'ok'


def test_lookup_demo_number():
    response = client.get('/v1/lookup', params={'number': '+14155550100'})
    assert response.status_code == 200
    assert response.json()['matches'][0]['name'] == 'Northstar Sample Cafe'
    assert response.json()['matches'][0]['verified'] in (False, 0)


def test_spam_report():
    response = client.post('/v1/reports/spam', json={'number': '+14155550199', 'category': 'nuisance'})
    assert response.status_code == 201
    assert response.json()['accepted'] is True


def test_duplicate_spam_report_from_same_network_is_deduplicated():
    payload = {'number': '+14155550198', 'category': 'telemarketing', 'details': 'Repeated call'}
    first = client.post('/v1/reports/spam', json=payload)
    second = client.post('/v1/reports/spam', json=payload)
    assert first.status_code == 201
    assert first.json()['accepted'] is True
    assert second.status_code == 201
    assert second.json()['accepted'] is False
    assert second.json()['report_count'] == 1


def test_import_disabled_without_admin_key():
    response = client.post('/v1/businesses', json={
        'number': '+14155550123',
        'name': 'Example Business',
        'category': 'Retail',
        'source': 'Licensed dataset',
        'source_license': 'Demo license'
    })
    assert response.status_code in (401, 503)


def test_reject_invalid_number():
    response = client.get('/v1/lookup', params={'number': 'not-a-number'})
    assert response.status_code == 400


def test_same_network_cannot_create_multiple_distinct_votes_via_categories():
    number = '+14155550197'
    client.post('/v1/reports/spam', json={'number': number, 'category': 'nuisance'})
    client.post('/v1/reports/spam', json={'number': number, 'category': 'other'})
    response = client.get('/v1/lookup', params={'number': number})
    assert response.status_code == 200
    assert response.json()['spam']['reports'] == 1


def test_lookup_returns_block_recommendation_after_five_distinct_installations():
    number = '+14155550196'
    for i in range(5):
        response = client.post(
            '/v1/reports/spam',
            headers={'X-Install-Token': f'test-install-token-{i:04d}'},
            json={'number': number, 'category': 'suspected_fraud', 'details': 'private detail'},
        )
        assert response.status_code == 201
    lookup = client.get('/v1/lookup', params={'number': number})
    assert lookup.status_code == 200
    payload = lookup.json()
    assert payload['spam']['reports'] == 5
    assert payload['spam']['recommended_action'] == 'block'
    assert 'details' not in str(payload)
    assert 'private detail' not in str(payload)


def test_install_token_deduplicates_across_report_categories():
    number = '+14155550195'
    headers = {'X-Install-Token': 'installation-token-123456789'}
    first = client.post('/v1/reports/spam', headers=headers, json={'number': number, 'category': 'nuisance'})
    second = client.post('/v1/reports/spam', headers=headers, json={'number': number, 'category': 'other'})
    assert first.status_code == 201 and first.json()['accepted'] is True
    assert second.status_code == 201 and second.json()['accepted'] is False


def test_bad_install_token_rejected():
    response = client.post(
        '/v1/reports/spam',
        headers={'X-Install-Token': 'short'},
        json={'number': '+14155550194', 'category': 'other'},
    )
    assert response.status_code == 400


def test_health_checks_database():
    response = client.get('/health')
    assert response.status_code == 200
    assert response.json()['database'] == 'ok'


def test_blank_business_fields_rejected_when_import_enabled(monkeypatch):
    import app.main as api
    monkeypatch.setattr(api, 'ADMIN_KEY', 'test-secret')
    response = client.post('/v1/businesses', headers={'X-Admin-Key': 'test-secret'}, json={
        'number': '+14155550193', 'name': '   ', 'category': 'Retail',
        'source': 'Licensed data', 'source_license': 'Terms allow reuse'
    })
    assert response.status_code == 400


def test_install_daily_report_limit():
    headers = {'X-Install-Token': 'daily-limit-test-install-12345'}
    for i in range(10):
        response = client.post('/v1/reports/spam', headers=headers, json={
            'number': f'+14155550{i:03d}', 'category': 'other'
        })
        assert response.status_code == 201
    limited = client.post('/v1/reports/spam', headers=headers, json={
        'number': '+14155550192', 'category': 'other'
    })
    assert limited.status_code == 429


def test_admin_import_requires_correct_key_and_preserves_license_provenance(monkeypatch):
    import app.main as api
    monkeypatch.setattr(api, 'ADMIN_KEY', 'test-import-secret')
    payload = {
        'number': '+14155550191',
        'name': 'Licensed Test Listing',
        'category': 'Retail',
        'source': 'Test Directory',
        'source_license': 'License permits redistribution'
    }
    denied = client.post('/v1/businesses', headers={'X-Admin-Key': 'wrong'}, json=payload)
    assert denied.status_code == 401
    accepted = client.post('/v1/businesses', headers={'X-Admin-Key': 'test-import-secret'}, json=payload)
    assert accepted.status_code == 201
    assert accepted.json()['accepted'] is True
    lookup = client.get('/v1/lookup', params={'number': payload['number']}).json()
    match = next(item for item in lookup['matches'] if item['name'] == payload['name'])
    assert 'license: License permits redistribution' in match['source']
    assert match['verified'] in (False, 0)


def test_search_is_limited_to_imported_public_business_records():
    response = client.get('/v1/search', params={'q': 'Northstar'})
    assert response.status_code == 200
    assert response.json()['results'][0]['name'] == 'Northstar Sample Cafe'
    assert 'reporter_hash' not in str(response.json())


def test_network_daily_limit_applies_even_when_install_tokens_rotate(monkeypatch):
    import app.main as api
    monkeypatch.setattr(api, 'REPORT_SALT', 'network-limit-isolated-test-salt')
    monkeypatch.setattr(api, 'REPORTS_PER_NETWORK_PER_DAY', 1)
    first = client.post('/v1/reports/spam', headers={'X-Install-Token': 'rotating-token-install-0001'}, json={
        'number': '+14155550190', 'category': 'other'
    })
    second = client.post('/v1/reports/spam', headers={'X-Install-Token': 'rotating-token-install-0002'}, json={
        'number': '+14155550189', 'category': 'other'
    })
    assert first.status_code == 201
    assert second.status_code == 429
