import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:stacked/stacked.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventPageModel extends BaseViewModel {
  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  List<EventModel> _originalEvents = [];
  List<EventModel> events = [];

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
      if (await canLaunchUrlString(event.infoLink.trim())) {
        launchUrlString(
          event.infoLink.trim(),
        );
      }
    };
  }
}
