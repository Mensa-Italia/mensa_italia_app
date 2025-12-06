import 'dart:ui';

import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddEventScheduleListViewModel extends MasterModel {
  @override
  String componentName = "views.add_event_schedule_list_viewmodel.title";
  final List<EventScheduleModel> eventSchedules;

  AddEventScheduleListViewModel({required this.eventSchedules});

  void onTapAddSchedule() {
    navigationService.navigateToAddScheduleView(previousPageTitle: componentName).then((value) {
      if (value != null && value is EventScheduleModel) {
        eventSchedules.add(value);
        eventSchedules.sort((a, b) => a.whenStart.compareTo(b.whenStart));
        rebuildUi();
      }
    });
  }

  VoidCallback tapEdit(EventScheduleModel event) {
    return () {
      if ((event.id ?? "").startsWith("DELETE:")) {
        return;
      }
      navigationService.navigateToAddScheduleView(event: event, previousPageTitle: componentName).then((value) {
        if (value != null && value is EventScheduleModel) {
          eventSchedules[eventSchedules.indexOf(event)] = value;
          eventSchedules.sort((a, b) => a.whenStart.compareTo(b.whenStart));
          rebuildUi();
        }
      });
    };
  }
}
