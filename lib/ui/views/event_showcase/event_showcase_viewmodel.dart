import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:maps_launcher/maps_launcher.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventShowcaseViewModel extends MasterModel {
  final EventModel event;
  final List<EventScheduleModel> eventSchedules = [];
  EventShowcaseViewModel({required this.event}) {
    load();
  }

  load() {
    Api().getEventSchedules(event.id).then((value) {
      eventSchedules.clear();
      eventSchedules.addAll(value);
      rebuildUi();
    });
  }

  void openMap() {
    MapsLauncher.launchCoordinates(event.position!.lat, event.position!.lon);
  }

  void openUrl() async {
    if (event.infoLink.trim().isNotEmpty &&
        await canLaunchUrlString(event.infoLink.trim())) {
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
}
