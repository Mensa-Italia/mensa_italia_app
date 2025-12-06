import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_filter/bottom_filter.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_filter/bottom_filter_model.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

class EventPageModel extends MasterModel {
  @override
  String componentName = "views.events.title";
  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  final List<EventModel> _originalEvents = [];
  final List<EventModel> events = [];
  String selectedState = "Nearby & Online";
  String type = "All";
  double distance = 20;

  Position? position;

  EventPageModel() {
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
    if (event.position != null && selectedState.contains("Online") && !selectedState.contains("Nearby")) {
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
      final distance = const Distance().distance(LatLng(position!.latitude, position!.longitude), event.position!.toLatLong2());
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
      navigationService.navigateToEventShowcaseView(previousPageTitle: componentName, event: event).then((value) {
        load();
      });
    };
  }

  void navigateToMap() {
    navigationService.navigateToEventsMapView(
      previousPageTitle: componentName,
    );
  }

  void navigateToAddEvent() {
    if (allowControlEvents()) {
      navigationService
          .navigateToAddEventView(
        previousPageTitle: componentName,
      )
          .then((value) {
        load();
      });
    } else {
      Api().canAddEvent().then((value) {
        if (value) {
          navigationService
              .navigateToAddEventView(
            previousPageTitle: componentName,
          )
              .then((value) {
            load();
          });
        } else {
          dialogService.showDialog(
            title: "views.events.add_event.dialog.title".tr(),
            description: "views.events.add_event.dialog.description".tr(),
            buttonTitle: "views.events.add_event.dialog.button".tr(),
          );
        }
      });
    }
  }

  void navigateToCalendar() {
    navigationService.navigateToEventCalendarView(
      previousPageTitle: componentName,
    );
  }

  void changeSearchRadius() async {
    showCupertinoModalBottomSheet(context: context, builder: (context) => BottomFilter());
  }

  Function() onLongTapEditEvent(EventModel? event) {
    return () async {
      navigationService.navigateToAddEventView(previousPageTitle: componentName, event: event).then((value) {
        load();
      });
    };
  }
}
