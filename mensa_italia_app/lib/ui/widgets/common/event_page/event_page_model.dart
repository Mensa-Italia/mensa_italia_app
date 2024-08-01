import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:stacked/stacked.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventPageModel extends BaseViewModel {
  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  List<EventModel> events = [];

  EventPageModel() {
    Api().getEvents().then((value) {
      events.clear();
      events.addAll(value);
      rebuildUi();
    });
  }

  void search(String value) {}

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
