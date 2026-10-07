import 'package:carousel_slider/carousel_slider.dart';
import 'package:ft_music/core/theme/app_theme.dart';
import 'package:ft_music/screens/screen/search_screen.dart';
import 'package:flutter/material.dart';

/// Offline-first category hero for the top of Discover. It performs no network
/// request until a category is tapped.
class HomeCategoryCarousel extends StatelessWidget {
  const HomeCategoryCarousel({super.key});

  static const _categories = <_HomeCategory>[
    _HomeCategory(
      title: 'Hits',
      subtitle: 'Global hits',
      query: 'Global Top Hits',
      icon: Icons.whatshot_rounded,
      colors: [Color(0xFF314D2A), Color(0xFFBFE98D)],
    ),
    _HomeCategory(
      title: 'English',
      subtitle: 'Pop, hip-hop & R&B',
      query: 'English Pop Hits',
      icon: Icons.language_rounded,
      colors: [Color(0xFF263E62), Color(0xFF6D9DDB)],
    ),
    _HomeCategory(
      title: 'Regional • North',
      subtitle: 'Hindi, Punjabi & Bengali',
      query: 'Hindi Punjabi Bengali hits',
      icon: Icons.north_rounded,
      colors: [Color(0xFF633B28), Color(0xFFE0A35B)],
    ),
    _HomeCategory(
      title: 'Regional • South',
      subtitle: 'Tamil, Telugu & Malayalam',
      query: 'South Indian regional hits',
      icon: Icons.south_rounded,
      colors: [Color(0xFF164B4A), Color(0xFF55C7A0)],
    ),
    _HomeCategory(
      title: 'Phonk',
      subtitle: 'Drift, Brazilian & chill',
      query: 'Phonk hits',
      icon: Icons.bolt_rounded,
      colors: [Color(0xFF321B4F), Color(0xFFAE6EE6)],
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final height = MediaQuery.sizeOf(context).width < 600 ? 300.0 : 340.0;
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: CarouselSlider.builder(
        itemCount: _categories.length,
        itemBuilder: (context, index, realIndex) {
          final category = _categories[index];
          return _CategoryHeroCard(category: category);
        },
        options: CarouselOptions(
          height: height,
          viewportFraction: MediaQuery.sizeOf(context).width < 600 ? .76 : .42,
          enlargeCenterPage: true,
          enlargeFactor: .16,
          autoPlay: true,
          autoPlayInterval: const Duration(seconds: 4),
          pauseAutoPlayOnTouch: true,
          enableInfiniteScroll: true,
        ),
      ),
    );
  }
}

class _HomeCategory {
  final String title;
  final String subtitle;
  final String query;
  final IconData icon;
  final List<Color> colors;

  const _HomeCategory({
    required this.title,
    required this.subtitle,
    required this.query,
    required this.icon,
    required this.colors,
  });
}

class _CategoryHeroCard extends StatelessWidget {
  final _HomeCategory category;
  const _CategoryHeroCard({required this.category});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => SearchScreen(searchQuery: category.query),
        ),
      ),
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(28),
          gradient: LinearGradient(
            colors: category.colors,
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
          ),
          boxShadow: [
            BoxShadow(
              color: category.colors.last.withValues(alpha: .24),
              blurRadius: 20,
              offset: const Offset(0, 10),
            ),
          ],
        ),
        clipBehavior: Clip.antiAlias,
        child: Stack(
          fit: StackFit.expand,
          children: [
            Positioned(
              right: -30,
              top: -38,
              child: Icon(
                category.icon,
                size: 190,
                color: Colors.white.withValues(alpha: .10),
              ),
            ),
            Positioned(
              left: 22,
              right: 22,
              bottom: 22,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 54,
                    height: 54,
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: .22),
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: Icon(category.icon, color: Colors.white, size: 29),
                  ),
                  const SizedBox(height: 18),
                  Text(
                    category.title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 25,
                      fontWeight: FontWeight.w900,
                      letterSpacing: -.5,
                    ).merge(AppTheme.secondoryTextStyleMedium),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    category.subtitle,
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: .82),
                      fontSize: 14,
                    ).merge(AppTheme.secondoryTextStyle),
                  ),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      Text(
                        'Explore now',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ).merge(AppTheme.secondoryTextStyleMedium),
                      ),
                      const SizedBox(width: 6),
                      const Icon(Icons.arrow_forward_rounded,
                          color: Colors.white, size: 18),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
