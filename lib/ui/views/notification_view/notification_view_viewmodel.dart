import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:pocketbase/pocketbase.dart';

class NotificationViewViewModel extends MasterModel {
  List<NotificationModel> notifications = [];
  UnsubscribeFunc? subscribe;

  NotificationViewViewModel() {
    setBusy(true);
    Api().pb.collection("user_notifications").subscribe("*", (e) {
      if (e.action == "create") {
        notifications.add(NotificationModel.fromJson(e.record!.toJson()));
      } else if (e.action == "update") {
        var index = notifications.indexWhere((element) => element.id == e.record!.id);
        if (index != -1) {
          notifications[index] = NotificationModel.fromJson(e.record!.toJson());
        } else {
          notifications.add(NotificationModel.fromJson(e.record!.toJson()));
        }
      } else if (e.action == "delete") {
        notifications.removeWhere((element) => element.id == e.record!.id);
      }
      notifications.sort((a, b) => b.created.compareTo(a.created));
      rebuildUi();
    }).then((value) {
      subscribe = value;
      Api().getNotifications().then((value) {
        notifications = value;
        notifications.sort((a, b) => b.created.compareTo(a.created));
        setBusy(false);
        rebuildUi();
      });
    });
  }

  void goToSettings() {
    navigationService.navigateToNotificationManagerView();
  }
}
