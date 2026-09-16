import 'dart:async';
import 'dart:developer';

import 'package:ft_music/core/models/app_notification.dart';
import 'package:ft_music/services/db/dao/notification_dao.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

part 'notification_state.dart';

class NotificationCubit extends Cubit<NotificationState> {
  final NotificationDAO _notificationDao;
  StreamSubscription? _subscription;

  NotificationCubit({required NotificationDAO notificationDao})
      : _notificationDao = notificationDao,
        super(NotificationInitial()) {
    getNotification();
  }
  void getNotification() async {
    final dbNotifications = await _notificationDao.getNotifications();
    final notifications = dbNotifications
        .map(
          (n) => AppNotification(
            title: n.title,
            body: n.body,
            type: n.type,
            url: n.url,
            payload: n.payload,
          ),
        )
        .toList(growable: false);
    emit(NotificationState(notifications: notifications));
  }

  void clearNotification() {
    _notificationDao.clearNotifications();
    log("Notification Cleared");
    getNotification();
  }

  Future<void> watchNotification() async {
    _subscription =
        (await _notificationDao.watchNotification()).listen((event) {
      getNotification();
    });
  }

  @override
  Future<void> close() {
    _subscription?.cancel();
    return super.close();
  }
}
