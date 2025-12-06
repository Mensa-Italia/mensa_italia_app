import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:mensa_italia_app/services/notify_sse.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:pocketbase/pocketbase.dart';

class NotificationViewViewModel extends MasterModel {
  @override
  String componentName = "views.notificaiton.title";
  List<NotificationModel> get notifications => NotifySSE().notifications;
  UnsubscribeFunc? subscribe;

  NotificationViewViewModel() {
    NotifySSE().addListener(rebuildUi);
  }

  @override
  void dispose() {
    NotifySSE().removeListener(rebuildUi);
    super.dispose();
  }

  void goToSettings() {
    navigationService.navigateToNotificationManagerView(
      previousPageTitle: componentName,);
  }
}
