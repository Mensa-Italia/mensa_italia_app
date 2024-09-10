import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/calendar_link.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:url_launcher/url_launcher_string.dart';

class CalendarLinkerViewModel extends MasterModel {
  CalendarLinkModel? calendarLink;

  CalendarLinkerViewModel() {
    load();
  }

  load() {
    Api().getCalendarLink().then((value) {
      calendarLink = value;
      rebuildUi();
    });
  }

  String get baseUrl => "//svc.mensa.it/ical/${calendarLink!.hash}";

  void addToCalendar() async {
    if (Theme.of(StackedService.navigatorKey!.currentContext!).platform ==
        TargetPlatform.iOS) {
      if (await canLaunchUrlString("webcal:$baseUrl")) {
        launchUrlString("webcal:$baseUrl");
      }
    } else {
      if (await canLaunchUrlString(
          "https://calendar.google.com/calendar/render?cid=webcal:${Uri.encodeQueryComponent(baseUrl)}")) {
        launchUrlString(
            "https://calendar.google.com/calendar/render?cid=webcal:${Uri.encodeQueryComponent(baseUrl)}");
      }
    }
  }

  String getPlatformUrl() {
    if (Theme.of(StackedService.navigatorKey!.currentContext!).platform ==
        TargetPlatform.iOS) {
      return "webcal:$baseUrl";
    } else {
      return "https://calendar.google.com/calendar/render?cid=webcal:${Uri.encodeQueryComponent(baseUrl)}";
    }
  }

  void copyToClipboard() async {
    print("Copying to clipboard");
    String url = "https:$baseUrl";
    Clipboard.setData(ClipboardData(text: url));
    Fluttertoast.showToast(
      msg: "Copied to clipboard",
      toastLength: Toast.LENGTH_SHORT,
      gravity: ToastGravity.BOTTOM,
      timeInSecForIosWeb: 1,
      backgroundColor: Colors.grey,
      textColor: Colors.white,
      fontSize: 16.0,
    );
  }

  bool hasState(String state) {
    return calendarLink != null && calendarLink!.state.contains(state);
  }

  void Function(bool value) changeState(String state) {
    return (value) {
      List<String> newState = calendarLink!.state.map((e) => e).toList();
      if (value) {
        newState.add(state);
      } else {
        newState = newState
          ..removeWhere(
              (element) => element.toLowerCase() == state.toLowerCase());
      }
      Api().changeCalendarLinkState(calendarLink!.id, newState).then((value) {
        calendarLink = value;
        rebuildUi();
      });
    };
  }
}
