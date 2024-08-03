import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
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

  EventPageModel() {
    Api().getEvents().then((value) {
      _originalEvents.clear();
      _originalEvents.addAll(value);
      events.clear();
      events.addAll(value);
      rebuildUi();
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
}
