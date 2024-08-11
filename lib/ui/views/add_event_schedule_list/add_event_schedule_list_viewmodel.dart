import 'dart:ui';

import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddEventScheduleListViewModel extends MasterModel {
  final List<EventScheduleModel> eventSchedules;

  AddEventScheduleListViewModel({required this.eventSchedules});

  void onTapAddSchedule() {
    navigationService.navigateToAddScheduleView().then((value) {
      if (value != null && value is EventScheduleModel) {
        eventSchedules.add(value);
        rebuildUi();
      }
    });
  }

  VoidCallback tapEdit(EventScheduleModel event) {
    return () {
      if ((event.id ?? "").startsWith("DELETE:")) {
        return;
      }
      navigationService.navigateToAddScheduleView(event: event).then((value) {
        if (value != null && value is EventScheduleModel) {
          eventSchedules[eventSchedules.indexOf(event)] = value;
          rebuildUi();
        }
      });
    };
  }
}
