import 'dart:async';
import 'dart:convert';

import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class HomeViewModel extends MasterModel {
  int currentIndex = 2;

  HomeViewModel() {
    //reviewApp();
    addNotificationPreference();
    setLanguageMetadata();
    showChangelog();
    checkForInitialMessage();
    listenForMessages();
  }

  //void reviewApp() async {
  //  SharedPreferences prefs = await SharedPreferences.getInstance();
  //  int review = prefs.getInt('review') ?? 0;
  //  if (review == 3) {
  //    final InAppReview inAppReview = InAppReview.instance;
  //    if (await inAppReview.isAvailable()) {
  //      inAppReview.requestReview();
  //      await prefs.setInt('review', review + 1);
  //    }
  //  } else {
  //    await prefs.setInt('review', review + 1);
  //  }
  //}

  void bottomBarTapped(int value) {
    currentIndex = value;
    rebuildUi();
  }

  void addNotificationPreference() {
    Api().getMetadata().then((metadata) async {
      try {
        List<String> notificationEvents =
            (jsonDecode(metadata["notify_me_events"] ?? "[]") as List<dynamic>)
                .cast<String>();
        if (notificationEvents.isNotEmpty) {
          return;
        }
      } catch (_) {}
      final position = await determinePosition();
      Api().locateState(position.latitude, position.longitude).then((value) {
        if (value == "NaN") {
          return;
        }
        Api().setMetadata("notify_me_events", jsonEncode([value]));
      });
    });
  }

  void setLanguageMetadata() {
    Api().setMetadata(
        "codes_locale", Localizations.localeOf(context).toString());
  }

  void checkForInitialMessage() async {
    RemoteMessage? initialMessage =
        await FirebaseMessaging.instance.getInitialMessage();
    String? internalID = initialMessage?.data["internal_id"];
    try {
      handleNotificationActions(initialMessage!.data,
          notificationID: internalID);
    } catch (_) {}
  }

  StreamSubscription? listenForMessagesvar;
  void listenForMessages() {
    listenForMessagesvar =
        FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      String? internalID = message.data["internal_id"];
      handleNotificationActions(message.data, notificationID: internalID);
    });
  }

  @override
  void dispose() {
    listenForMessagesvar?.cancel();
    super.dispose();
  }
}
