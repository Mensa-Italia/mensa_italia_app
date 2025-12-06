import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_filter/bottom_filter.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_filter/bottom_filter_model.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:table_calendar/table_calendar.dart';

class EventCalendarViewModel extends MasterModel {
  @override
  String componentName = "views.event_calendar.title";
  List<EventModel> events = [];
  DateTime selectedDate = DateTime.now();
  String selectedState = "Nearby & Online";
  Position? position;
  double distance = 20;
  String type = "All";
  List<EventModel> _originalEvents = [];

  EventCalendarViewModel() {
    load();
    FilterNotification().addListener(load);
  }

  bool matchEventType(EventModel event) {
    if (type == "All") {
      return true;
    }
    if (type == "International") {
      return event.position?.state == "NaN";
    }
    if (type == "National") {
      return event.isNational;
    }
    if (type == "Local") {
      return !event.isNational && !event.isSpot;
    }
    if (type == "Spot") {
      return event.isSpot;
    }
    return false;
  }

  bool matchState(EventModel event) {
    if (type == "International") {
      return event.position?.state == "NaN";
    }
    if (selectedState == "All") {
      return true;
    }
    if (event.position == null && selectedState.contains("Online")) {
      return true;
    }
    if (event.position != null &&
        selectedState.contains("Online") &&
        !selectedState.contains("Nearby")) {
      return false;
    }
    if (!selectedState.contains("Nearby")) {
      return event.position?.state == selectedState;
    } else {
      if (event.isNational) {
        return true;
      }
    }
    if (position == null) {
      return false;
    } else {
      if (event.position == null) {
        return false;
      }
      final distance = const Distance().distance(
          LatLng(position!.latitude, position!.longitude),
          event.position!.toLatLong2());
      return distance < this.distance * 1000;
    }
  }

  load() async {
    Api().getMetadata().then((value) async {
      selectedState = value["eventfilter_state"] ?? "Nearby & Online";
      type = value["eventfilter_type"] ?? "All";
      distance = double.parse((value["eventfilter_distance"] ?? "20"));
      try {
        position ??= await determinePosition();
      } catch (_) {}
      Api().getEvents().then((value) {
        _originalEvents.clear();
        _originalEvents.addAll(value);
        events.clear();
        events.addAll(value.where((element) {
          return matchState(element) && matchEventType(element);
        }));
        rebuildUi();
      });
      events.sort(
        (a, b) {
          if (a.isNational && !b.isNational) {
            return -1;
          }
          if (!a.isNational && b.isNational) {
            return 1;
          }
          return a.whenStart.compareTo(b.whenStart);
        },
      );
    });
  }

  bool isSelectedDay(DateTime day) {
    return isSameDay(selectedDate, day);
  }

  void onDaySelected(DateTime selectedDay, DateTime focusedDay) {
    selectedDate = selectedDay;
    rebuildUi();
  }

  List<EventModel> selectedDateEvents() {
    return events
        .where((element) =>
            isDateBetween(selectedDate, element.whenStart, element.whenEnd))
        .toList();
  }

  List retrieveEvents(DateTime day) {
    return events
        .where(
            (element) => isDateBetween(day, element.whenStart, element.whenEnd))
        .map((e) => e.name)
        .toList();
  }

  List<EventModel> retrieveDateEvents(DateTime day) {
    return events
        .where(
            (element) => isDateBetween(day, element.whenStart, element.whenEnd))
        .toList();
  }

  Function() onTapOnEvent(EventModel event) {
    return () async {
      navigationService.navigateToEventShowcaseView(event: event,
      previousPageTitle: componentName,);
    };
  }

  void changeSearchRadius() async {
    showCupertinoModalBottomSheet(
        context: context, builder: (context) => BottomFilter());
  }
}

bool isDateBetween(DateTime date, DateTime start, DateTime end) {
  return (date.isAfter(normalizeDate(start)) &&
          date.isBefore(normalizeDate(end))) ||
      isSameDay(date, start) ||
      isSameDay(date, end);
}
