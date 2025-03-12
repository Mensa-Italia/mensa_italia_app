import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class NotificationViewViewModel extends MasterModel {
  List<NotificationModel> notifications = [];

  NotificationViewViewModel() {
    setBusy(true);
    Api().getNotifications().then((value) {
      notifications = value;
      for (var notification in notifications) {
        Api().seeNotification(notification.id);
      }
      setBusy(false);
    });
  }

  void goToSettings() {
    navigationService.navigateToNotificationManagerView();
  }
}
