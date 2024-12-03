import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_filter/bottom_filter.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_filter/bottom_filter_model.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';

class EventPageModel extends MasterModel {
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
          if (selectedState == "All") {
            return true;
          }
          if (element.position == null && selectedState.contains("Online")) {
            return true;
          }
          if (element.position != null && selectedState.contains("Online") && !selectedState.contains("Nearby")) {
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
            final distance = const Distance().distance(LatLng(position!.latitude, position!.longitude), element.position!.toLatLong2());
            return distance < this.distance * 1000;
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
      navigationService.navigateToEventShowcaseView(event: event).then((value) {
        load();
      });
    };
  }

  void navigateToMap() {
    navigationService.navigateToEventsMapView();
  }

  void navigateToAddEvent() {
    if (allowControlEvents()) {
      navigationService.navigateToAddEventView().then((value) {
        load();
      });
    } else {
      Api().canAddEvent().then((value) {
        if (value) {
          navigationService.navigateToAddEventView().then((value) {
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
    navigationService.navigateToEventCalendarView();
  }

  void changeSearchRadius() async {
    showCupertinoModalBottomSheet(context: context, builder: (context) => BottomFilter());
  }

  Function() onLongTapEditEvent(EventModel event) {
    return () async {
      navigationService.navigateToAddEventView(event: event).then((value) {
        load();
      });
    };
  }
}
