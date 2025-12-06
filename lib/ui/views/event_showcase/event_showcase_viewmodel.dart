import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/event_owner.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:maps_launcher/maps_launcher.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventShowcaseViewModel extends MasterModel {
  @override
  String componentName = "views.event_showcase.title";

  EventModel event;
  final List<EventScheduleModel> eventSchedules = [];
  List<EventOwnerModel>? owners;
  EventShowcaseViewModel({required this.event}) {
    load();
    Api().getEventOwner(event.id).then((value) {
      owners = value;
      rebuildUi();
    });
  }

  load() {
    Api().getEvent(event.id).then((value) {
      event = value;
      Api().getEventSchedules(event.id).then((value) {
        eventSchedules.clear();
        eventSchedules.addAll(value..sort((a, b) => a.whenStart.compareTo(b.whenStart)));
        rebuildUi();
      });
    });
  }

  void openMap() {
    if (event.position == null) {
      return;
    }
    MapsLauncher.launchCoordinates(event.position!.lat, event.position!.lon);
  }

  void openUrl() async {
    if (event.infoLink.trim().isNotEmpty && await canLaunchUrlString(event.infoLink.trim())) {
      launchUrlString(
        event.infoLink.trim(),
      );
    } else {
      dialogService.showDialog(
        title: 'Not ready yet',
        description: 'This event is being prepared, please try again later.',
      );
    }
  }

  void editEvent() {
    navigationService.navigateToAddEventView(event: event,
      previousPageTitle: componentName,).then((value) {
      load();
    });
  }

  void shareEvent() {
    SharePlus.instance.share(
      ShareParams(
        uri: Uri.parse("https://svc.mensa.it/links/event/${event.id}"),
      ),
    );
  }
}
