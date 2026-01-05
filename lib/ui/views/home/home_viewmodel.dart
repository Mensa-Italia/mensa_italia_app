import 'dart:async';
import 'dart:convert';

import 'package:app_links/app_links.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/services/tickets_see%20.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/addon_stamp/addon_stamp_viewmodel.dart';

class HomeViewModel extends MasterModel {
  @override
  String componentName = "views.home.title";
  int currentIndex = 2;

  HomeViewModel() {
    //reviewApp();
    TicketSSE().start();
    addNotificationPreference();
    setLanguageMetadata();
    showChangelog();
    checkForInitialMessage();
    listenForMessages();
    TicketSSE().addListener(rebuildUi);
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
        List<String> notificationEvents = (jsonDecode(metadata["notify_me_events"] ?? "[]") as List<dynamic>).cast<String>();
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
    Api().setMetadata("codes_locale", Localizations.localeOf(context).toString());
  }

  void checkForInitialMessage() async {
    RemoteMessage? initialMessage = await FirebaseMessaging.instance.getInitialMessage();
    String? internalID = initialMessage?.data["internal_id"];
    try {
      handleNotificationActions(initialMessage!.data, notificationID: internalID, componentName: componentName);
    } catch (_) {}
  }

  StreamSubscription? listenForMessagesvar;
  StreamSubscription? appLinksSub;
  void listenForMessages() {
    listenForMessagesvar = FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      String? internalID = message.data["internal_id"];
      handleNotificationActions(message.data, notificationID: internalID, componentName: componentName);
    }); // AppLinks is singleton
    final appLinks = AppLinks();
    appLinksSub = appLinks.uriLinkStream.listen((uri) {
      if (uri.pathSegments.isNotEmpty && uri.pathSegments.first == "links" && uri.pathSegments.length > 1 && uri.pathSegments[1] == "event") {
        String eventId = uri.pathSegments[2];
        Api().getEvent(eventId).then((event) {
          navigationService.navigateToEventShowcaseView(
            event: event,
            previousPageTitle: componentName,
          );
        });
      } else if (uri.pathSegments.isNotEmpty && uri.pathSegments.first == "links" && uri.pathSegments.length > 1 && uri.pathSegments[1] == "stamp") {
        String qrCode = uri.pathSegments[2];
        String idStamp = "";
        String codeStamp = "";
        try {
          final valuesScanned = qrCode.split(":::");
          idStamp = valuesScanned[0];
          codeStamp = valuesScanned[1];
        } catch (e) {
          return;
        }
        showBeautifulBottomSheet(
          child: AddStampModal(idStamp: idStamp, codeStamp: codeStamp),
        ).then((value) {
          if (value == true) {
            if (navigationService.currentRoute == Routes.addonStampView) {
              navigationService.back();
            }
            navigationService.navigateToAddonStampView(previousPageTitle: componentName);
          }
        });
      }
    });
  }

  @override
  void dispose() {
    listenForMessagesvar?.cancel();
    TicketSSE().removeListener(rebuildUi);
    super.dispose();
  }
}
