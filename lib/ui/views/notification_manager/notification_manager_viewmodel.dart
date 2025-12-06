import 'dart:convert';

import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class NotificationManagerViewModel extends MasterModel {


  @override
  String componentName = "views.notification_manager.title";
  List<String> notificationEvents = [];

  NotificationManagerViewModel() {
    Api().getMetadata().then((metadata) {
      try {
        notificationEvents =
            (jsonDecode(metadata["notify_me_events"] ?? "[]") as List<dynamic>)
                .cast<String>();
        rebuildUi();
      } catch (_) {}
    });
  }
  hasState(String state) {
    return notificationEvents.contains(state);
  }

  void Function(bool) changeState(String state) {
    return (bool value) {
      if (value) {
        notificationEvents.add(state);
      } else {
        notificationEvents.remove(state);
      }
      Api().setMetadata("notify_me_events", jsonEncode(notificationEvents));
      rebuildUi();
    };
  }
}
