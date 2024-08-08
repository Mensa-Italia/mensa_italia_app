import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventPageModel extends MasterModel {
  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  final List<EventModel> _originalEvents = [];
  final List<EventModel> events = [];
  Position? position;

  EventPageModel() {
    load();
  }

  load() async {
    try {
      position = await determinePosition();
    } catch (_) {}
    Api().getEvents().then((value) {
      _originalEvents.clear();
      _originalEvents.addAll(value);
      events.clear();
      events.addAll(value.where((element) {
        if (element.isNational) {
          return true;
        }
        if (position == null) {
          return false;
        } else {
          final distance = const Distance().distance(LatLng(position!.latitude, position!.longitude), element.position!.toLatLong2());
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
        return a.when.compareTo(b.when);
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

  void navigateToMap() {
    navigationService.navigateToEventsMapView();
  }

  void navigateToAddEvent() {
    navigationService.navigateToAddEventView();
  }

  void navigateToCalendar() {
    navigationService.navigateToEventCalendarView();
  }

  void changeSearchRadius() {
    dialogService.showDialog(
      title: 'Your location',
      description: 'We are using your location to show you events near you. You can use the map above to see events in other locations.',
    );
  }
}
