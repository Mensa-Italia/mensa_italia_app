import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventCalendarViewModel extends MasterModel {
  List<EventModel> events = [];
  DateTime selectedDate = DateTime.now();

  load() {
    Api().getEvents().then((value) {
      events = value;
      rebuildUi();
    });
  }

  EventCalendarViewModel() {
    load();
  }

  bool isSelectedDay(DateTime day) {
    return isSameDay(selectedDate, day);
  }

  void onDaySelected(DateTime selectedDay, DateTime focusedDay) {
    selectedDate = selectedDay;
    rebuildUi();
  }

  List<EventModel> selectedDateEvents() {
    return events.where((element) => isSameDay(element.when, selectedDate)).toList();
  }

  List retrieveEvents(DateTime day) {
    return events.where((element) => isSameDay(element.when, day)).map((e) => e.name).toList();
  }

  Function() onTapOnEvent(EventModel event) {
    return () async {
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
    };
  }
}
