import 'package:flutter/cupertino.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class EventPageModel extends MasterModel {
  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  final List<EventModel> _originalEvents = [];
  final List<EventModel> events = [];
  String selectedState = "Nearby & Online";
  Position? position;

  EventPageModel() {
    load();
  }

  load() async {
    try {
      position ??= await determinePosition();
    } catch (_) {}
    Api().getEvents().then((value) {
      _originalEvents.clear();
      _originalEvents.addAll(value);
      events.clear();
      events.addAll(value.where((element) {
        if (selectedState == "All") {
          return true;
        }
        if (element.position == null && selectedState.contains("Online")) {
          return true;
        }
        if (element.position != null &&
            selectedState.contains("Online") &&
            !selectedState.contains("Nearby")) {
          return false;
        }
        if (!selectedState.contains("Nearby")) {
          return element.position?.state == selectedState;
        } else {
          if (element.isNational) {
            return true;
          }
        }
        if (position == null) {
          return false;
        } else {
          if (element.position == null) {
            return false;
          }
          final distance = const Distance().distance(
              LatLng(position!.latitude, position!.longitude),
              element.position!.toLatLong2());
          return distance < 90000;
        }
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
  }

  void search(String value) {
    if (value.isEmpty) {
      events.clear();
      events.addAll(_originalEvents);
      rebuildUi();
      return;
    }
    events.clear();
    events.addAll(_originalEvents.where((element) {
      return element.name.toLowerCase().contains(value.toLowerCase());
    }));
    rebuildUi();
  }

  Function() onTapOnEvent(EventModel event) {
    return () async {
      navigationService.navigateToEventShowcaseView(event: event);
    };
  }

  void navigateToMap() {
    navigationService.navigateToEventsMapView();
  }

  void navigateToAddEvent() {
    navigationService.navigateToAddEventView().then((value) {
      load();
    });
  }

  void navigateToCalendar() {
    navigationService.navigateToEventCalendarView();
  }

  void changeSearchRadius() async {
    final UsableListOfStates = [
      "Nearby & Online",
      "Nearby",
      "Online",
      "All",
      ...ListOfStates
    ];

    cupertinoModalPicker(
      initialItem: UsableListOfStates.indexOf(selectedState),
      items: UsableListOfStates,
    ).then((value) {
      if (value != null) {
        selectedState = value;
        load();
      }
    });
  }

  Function() onLongTapEditEvent(EventModel event) {
    return () async {
      navigationService.navigateToAddEventView(event: event).then((value) {
        load();
      });
    };
  }
}
