import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:pocketbase/pocketbase.dart';

class NotifySSE extends ChangeNotifier {
  static final NotifySSE _instance = NotifySSE._internal();
  factory NotifySSE() => _instance;
  NotifySSE._internal();

  List<NotificationModel> _data = [];

  UnsubscribeFunc? _unsubscribe;

  List<NotificationModel> get notifications => _data;

  int get unseenNotifications =>
      _data.where((element) => (element.seen == null)).length;

  void start() {
    Api().pb.collection("user_notifications").subscribe("*", (e) {
      if (e.action == "create") {
        _data.add(NotificationModel.fromJson(e.record!.toJson()));
      } else if (e.action == "update") {
        var index = _data.indexWhere((element) => element.id == e.record!.id);
        if (index != -1) {
          _data[index] = NotificationModel.fromJson(e.record!.toJson());
        } else {
          _data.add(NotificationModel.fromJson(e.record!.toJson()));
        }
      } else if (e.action == "delete") {
        _data.removeWhere((element) => element.id == e.record!.id);
      }
      _data.sort((a, b) => b.created.compareTo(a.created));
      notifyListeners();
    }).then((value) {
      _unsubscribe = value;
      Api().getNotifications().then((value) {
        _data.addAll(value);
        _data.sort((a, b) => b.created.compareTo(a.created));
        notifyListeners();
      });
    });
  }

  void stop() {
    _unsubscribe?.call();
  }
}
