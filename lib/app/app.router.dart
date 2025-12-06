// dart format width=80
// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedNavigatorGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart' as _i35;
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/boutique.dart' as _i39;
import 'package:mensa_italia_app/model/deal.dart' as _i38;
import 'package:mensa_italia_app/model/document.dart' as _i40;
import 'package:mensa_italia_app/model/event.dart' as _i36;
import 'package:mensa_italia_app/model/event_schedule.dart' as _i37;
import 'package:mensa_italia_app/ui/views/add_event/add_event_view.dart'
    as _i12;
import 'package:mensa_italia_app/ui/views/add_event_schedule_list/add_event_schedule_list_view.dart'
    as _i18;
import 'package:mensa_italia_app/ui/views/add_schedule/add_schedule_view.dart'
    as _i19;
import 'package:mensa_italia_app/ui/views/addon_area_documents/addon_area_documents_view.dart'
    as _i10;
import 'package:mensa_italia_app/ui/views/addon_area_documents_preview/addon_area_documents_preview_view.dart'
    as _i30;
import 'package:mensa_italia_app/ui/views/addon_boutique/addon_boutique_view.dart'
    as _i28;
import 'package:mensa_italia_app/ui/views/addon_boutique_product/addon_boutique_product_view.dart'
    as _i29;
import 'package:mensa_italia_app/ui/views/addon_contacts/addon_contacts_view.dart'
    as _i6;
import 'package:mensa_italia_app/ui/views/addon_deals/addon_deals_view.dart'
    as _i20;
import 'package:mensa_italia_app/ui/views/addon_deals_add/addon_deals_add_view.dart'
    as _i22;
import 'package:mensa_italia_app/ui/views/addon_deals_details/addon_deals_details_view.dart'
    as _i21;
import 'package:mensa_italia_app/ui/views/addon_stamp/addon_stamp_view.dart'
    as _i23;
import 'package:mensa_italia_app/ui/views/addon_test_assistant/addon_test_assistant_view.dart'
    as _i9;
import 'package:mensa_italia_app/ui/views/calendar_linker/calendar_linker_view.dart'
    as _i16;
import 'package:mensa_italia_app/ui/views/devices/devices_view.dart' as _i32;
import 'package:mensa_italia_app/ui/views/document_viewer/document_viewer_view.dart'
    as _i14;
import 'package:mensa_italia_app/ui/views/event_calendar/event_calendar_view.dart'
    as _i15;
import 'package:mensa_italia_app/ui/views/event_showcase/event_showcase_view.dart'
    as _i17;
import 'package:mensa_italia_app/ui/views/events_map/events_map_view.dart'
    as _i11;
import 'package:mensa_italia_app/ui/views/external_addon_webview/external_addon_webview_view.dart'
    as _i5;
import 'package:mensa_italia_app/ui/views/generic_webview/generic_webview_view.dart'
    as _i8;
import 'package:mensa_italia_app/ui/views/home/home_view.dart' as _i4;
import 'package:mensa_italia_app/ui/views/location_list_picker/location_list_picker_view.dart'
    as _i33;
import 'package:mensa_italia_app/ui/views/login/login_view.dart' as _i2;
import 'package:mensa_italia_app/ui/views/make_donation/make_donation_view.dart'
    as _i26;
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_view.dart'
    as _i13;
import 'package:mensa_italia_app/ui/views/notification_manager/notification_manager_view.dart'
    as _i24;
import 'package:mensa_italia_app/ui/views/notification_view/notification_view_view.dart'
    as _i31;
import 'package:mensa_italia_app/ui/views/payment_method_manager/payment_method_manager_view.dart'
    as _i25;
import 'package:mensa_italia_app/ui/views/receipts/receipts_view.dart' as _i27;
import 'package:mensa_italia_app/ui/views/renew_membership/renew_membership_view.dart'
    as _i7;
import 'package:mensa_italia_app/ui/views/startup/startup_view.dart' as _i3;
import 'package:mensa_italia_app/ui/views/tickets/tickets_view.dart' as _i34;
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i41;

class Routes {
  static const loginView = '/login-view';

  static const startupView = '/startup-view';

  static const homeView = '/home-view';

  static const externalAddonWebviewView = '/external-addon-webview-view';

  static const addonContactsView = '/addon-contacts-view';

  static const renewMembershipView = '/renew-membership-view';

  static const genericWebviewView = '/generic-webview-view';

  static const addonTestAssistantView = '/addon-test-assistant-view';

  static const addonAreaDocumentsView = '/addon-area-documents-view';

  static const eventsMapView = '/events-map-view';

  static const addEventView = '/add-event-view';

  static const mapPickerView = '/map-picker-view';

  static const documentViewerView = '/document-viewer-view';

  static const eventCalendarView = '/event-calendar-view';

  static const calendarLinkerView = '/calendar-linker-view';

  static const eventShowcaseView = '/event-showcase-view';

  static const addEventScheduleListView = '/add-event-schedule-list-view';

  static const addScheduleView = '/add-schedule-view';

  static const addonDealsView = '/addon-deals-view';

  static const addonDealsDetailsView = '/addon-deals-details-view';

  static const addonDealsAddView = '/addon-deals-add-view';

  static const addonStampView = '/addon-stamp-view';

  static const notificationManagerView = '/notification-manager-view';

  static const paymentMethodManagerView = '/payment-method-manager-view';

  static const makeDonationView = '/make-donation-view';

  static const receiptsView = '/receipts-view';

  static const addonBoutiqueView = '/addon-boutique-view';

  static const addonBoutiqueProductView = '/addon-boutique-product-view';

  static const addonAreaDocumentsPreviewView =
      '/addon-area-documents-preview-view';

  static const notificationViewView = '/notification-view-view';

  static const devicesView = '/devices-view';

  static const locationListPickerView = '/location-list-picker-view';

  static const ticketsView = '/tickets-view';

  static const all = <String>{
    loginView,
    startupView,
    homeView,
    externalAddonWebviewView,
    addonContactsView,
    renewMembershipView,
    genericWebviewView,
    addonTestAssistantView,
    addonAreaDocumentsView,
    eventsMapView,
    addEventView,
    mapPickerView,
    documentViewerView,
    eventCalendarView,
    calendarLinkerView,
    eventShowcaseView,
    addEventScheduleListView,
    addScheduleView,
    addonDealsView,
    addonDealsDetailsView,
    addonDealsAddView,
    addonStampView,
    notificationManagerView,
    paymentMethodManagerView,
    makeDonationView,
    receiptsView,
    addonBoutiqueView,
    addonBoutiqueProductView,
    addonAreaDocumentsPreviewView,
    notificationViewView,
    devicesView,
    locationListPickerView,
    ticketsView,
  };
}

class StackedRouter extends _i1.RouterBase {
  final _routes = <_i1.RouteDef>[
    _i1.RouteDef(Routes.loginView, page: _i2.LoginView),
    _i1.RouteDef(Routes.startupView, page: _i3.StartupView),
    _i1.RouteDef(Routes.homeView, page: _i4.HomeView),
    _i1.RouteDef(
      Routes.externalAddonWebviewView,
      page: _i5.ExternalAddonWebviewView,
    ),
    _i1.RouteDef(Routes.addonContactsView, page: _i6.AddonContactsView),
    _i1.RouteDef(Routes.renewMembershipView, page: _i7.RenewMembershipView),
    _i1.RouteDef(Routes.genericWebviewView, page: _i8.GenericWebviewView),
    _i1.RouteDef(
      Routes.addonTestAssistantView,
      page: _i9.AddonTestAssistantView,
    ),
    _i1.RouteDef(
      Routes.addonAreaDocumentsView,
      page: _i10.AddonAreaDocumentsView,
    ),
    _i1.RouteDef(Routes.eventsMapView, page: _i11.EventsMapView),
    _i1.RouteDef(Routes.addEventView, page: _i12.AddEventView),
    _i1.RouteDef(Routes.mapPickerView, page: _i13.MapPickerView),
    _i1.RouteDef(Routes.documentViewerView, page: _i14.DocumentViewerView),
    _i1.RouteDef(Routes.eventCalendarView, page: _i15.EventCalendarView),
    _i1.RouteDef(Routes.calendarLinkerView, page: _i16.CalendarLinkerView),
    _i1.RouteDef(Routes.eventShowcaseView, page: _i17.EventShowcaseView),
    _i1.RouteDef(
      Routes.addEventScheduleListView,
      page: _i18.AddEventScheduleListView,
    ),
    _i1.RouteDef(Routes.addScheduleView, page: _i19.AddScheduleView),
    _i1.RouteDef(Routes.addonDealsView, page: _i20.AddonDealsView),
    _i1.RouteDef(
      Routes.addonDealsDetailsView,
      page: _i21.AddonDealsDetailsView,
    ),
    _i1.RouteDef(Routes.addonDealsAddView, page: _i22.AddonDealsAddView),
    _i1.RouteDef(Routes.addonStampView, page: _i23.AddonStampView),
    _i1.RouteDef(
      Routes.notificationManagerView,
      page: _i24.NotificationManagerView,
    ),
    _i1.RouteDef(
      Routes.paymentMethodManagerView,
      page: _i25.PaymentMethodManagerView,
    ),
    _i1.RouteDef(Routes.makeDonationView, page: _i26.MakeDonationView),
    _i1.RouteDef(Routes.receiptsView, page: _i27.ReceiptsView),
    _i1.RouteDef(Routes.addonBoutiqueView, page: _i28.AddonBoutiqueView),
    _i1.RouteDef(
      Routes.addonBoutiqueProductView,
      page: _i29.AddonBoutiqueProductView,
    ),
    _i1.RouteDef(
      Routes.addonAreaDocumentsPreviewView,
      page: _i30.AddonAreaDocumentsPreviewView,
    ),
    _i1.RouteDef(Routes.notificationViewView, page: _i31.NotificationViewView),
    _i1.RouteDef(Routes.devicesView, page: _i32.DevicesView),
    _i1.RouteDef(
      Routes.locationListPickerView,
      page: _i33.LocationListPickerView,
    ),
    _i1.RouteDef(Routes.ticketsView, page: _i34.TicketsView),
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.LoginView: (data) {
      final args = data.getArgs<LoginViewArguments>(
        orElse: () => const LoginViewArguments(),
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i2.LoginView(key: args.key),
        settings: data,
      );
    },
    _i3.StartupView: (data) {
      final args = data.getArgs<StartupViewArguments>(
        orElse: () => const StartupViewArguments(),
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i3.StartupView(key: args.key),
        settings: data,
      );
    },
    _i4.HomeView: (data) {
      final args = data.getArgs<HomeViewArguments>(
        orElse: () => const HomeViewArguments(),
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i4.HomeView(key: args.key),
        settings: data,
      );
    },
    _i5.ExternalAddonWebviewView: (data) {
      final args = data.getArgs<ExternalAddonWebviewViewArguments>(
        nullOk: false,
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i5.ExternalAddonWebviewView(
          key: args.key,
          addonID: args.addonID,
          addonURL: args.addonURL,
        ),
        settings: data,
      );
    },
    _i6.AddonContactsView: (data) {
      final args = data.getArgs<AddonContactsViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i6.AddonContactsView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i7.RenewMembershipView: (data) {
      final args = data.getArgs<RenewMembershipViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i7.RenewMembershipView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i8.GenericWebviewView: (data) {
      final args = data.getArgs<GenericWebviewViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i8.GenericWebviewView(
          key: args.key,
          url: args.url,
          title: args.title,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i9.AddonTestAssistantView: (data) {
      final args = data.getArgs<AddonTestAssistantViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i9.AddonTestAssistantView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i10.AddonAreaDocumentsView: (data) {
      final args = data.getArgs<AddonAreaDocumentsViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i10.AddonAreaDocumentsView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i11.EventsMapView: (data) {
      final args = data.getArgs<EventsMapViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i11.EventsMapView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i12.AddEventView: (data) {
      final args = data.getArgs<AddEventViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i12.AddEventView(
          key: args.key,
          event: args.event,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i13.MapPickerView: (data) {
      final args = data.getArgs<MapPickerViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i13.MapPickerView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i14.DocumentViewerView: (data) {
      final args = data.getArgs<DocumentViewerViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i14.DocumentViewerView(
          key: args.key,
          downlaodUrl: args.downlaodUrl,
          title: args.title,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i15.EventCalendarView: (data) {
      final args = data.getArgs<EventCalendarViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i15.EventCalendarView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i16.CalendarLinkerView: (data) {
      final args = data.getArgs<CalendarLinkerViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i16.CalendarLinkerView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i17.EventShowcaseView: (data) {
      final args = data.getArgs<EventShowcaseViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i17.EventShowcaseView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
          event: args.event,
        ),
        settings: data,
      );
    },
    _i18.AddEventScheduleListView: (data) {
      final args = data.getArgs<AddEventScheduleListViewArguments>(
        nullOk: false,
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i18.AddEventScheduleListView(
          key: args.key,
          eventSchedules: args.eventSchedules,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i19.AddScheduleView: (data) {
      final args = data.getArgs<AddScheduleViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i19.AddScheduleView(
          key: args.key,
          event: args.event,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i20.AddonDealsView: (data) {
      final args = data.getArgs<AddonDealsViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i20.AddonDealsView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i21.AddonDealsDetailsView: (data) {
      final args = data.getArgs<AddonDealsDetailsViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i21.AddonDealsDetailsView(
          key: args.key,
          deal: args.deal,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i22.AddonDealsAddView: (data) {
      final args = data.getArgs<AddonDealsAddViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i22.AddonDealsAddView(
          key: args.key,
          deal: args.deal,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i23.AddonStampView: (data) {
      final args = data.getArgs<AddonStampViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i23.AddonStampView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i24.NotificationManagerView: (data) {
      final args = data.getArgs<NotificationManagerViewArguments>(
        nullOk: false,
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i24.NotificationManagerView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i25.PaymentMethodManagerView: (data) {
      final args = data.getArgs<PaymentMethodManagerViewArguments>(
        nullOk: false,
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i25.PaymentMethodManagerView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i26.MakeDonationView: (data) {
      final args = data.getArgs<MakeDonationViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i26.MakeDonationView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i27.ReceiptsView: (data) {
      final args = data.getArgs<ReceiptsViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i27.ReceiptsView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i28.AddonBoutiqueView: (data) {
      final args = data.getArgs<AddonBoutiqueViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i28.AddonBoutiqueView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i29.AddonBoutiqueProductView: (data) {
      final args = data.getArgs<AddonBoutiqueProductViewArguments>(
        nullOk: false,
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i29.AddonBoutiqueProductView(
          key: args.key,
          product: args.product,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i30.AddonAreaDocumentsPreviewView: (data) {
      final args = data.getArgs<AddonAreaDocumentsPreviewViewArguments>(
        nullOk: false,
      );
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i30.AddonAreaDocumentsPreviewView(
          key: args.key,
          document: args.document,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i31.NotificationViewView: (data) {
      final args = data.getArgs<NotificationViewViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i31.NotificationViewView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i32.DevicesView: (data) {
      final args = data.getArgs<DevicesViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i32.DevicesView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i33.LocationListPickerView: (data) {
      final args = data.getArgs<LocationListPickerViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i33.LocationListPickerView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
    _i34.TicketsView: (data) {
      final args = data.getArgs<TicketsViewArguments>(nullOk: false);
      return _i35.MaterialPageRoute<dynamic>(
        builder: (context) => _i34.TicketsView(
          key: args.key,
          previousPageTitle: args.previousPageTitle,
        ),
        settings: data,
      );
    },
  };

  @override
  List<_i1.RouteDef> get routes => _routes;

  @override
  Map<Type, _i1.StackedRouteFactory> get pagesMap => _pagesMap;
}

class LoginViewArguments {
  const LoginViewArguments({this.key});

  final _i35.Key? key;

  @override
  String toString() {
    return '{"key": "$key"}';
  }

  @override
  bool operator ==(covariant LoginViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key;
  }

  @override
  int get hashCode {
    return key.hashCode;
  }
}

class StartupViewArguments {
  const StartupViewArguments({this.key});

  final _i35.Key? key;

  @override
  String toString() {
    return '{"key": "$key"}';
  }

  @override
  bool operator ==(covariant StartupViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key;
  }

  @override
  int get hashCode {
    return key.hashCode;
  }
}

class HomeViewArguments {
  const HomeViewArguments({this.key});

  final _i35.Key? key;

  @override
  String toString() {
    return '{"key": "$key"}';
  }

  @override
  bool operator ==(covariant HomeViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key;
  }

  @override
  int get hashCode {
    return key.hashCode;
  }
}

class ExternalAddonWebviewViewArguments {
  const ExternalAddonWebviewViewArguments({
    this.key,
    required this.addonID,
    required this.addonURL,
  });

  final _i35.Key? key;

  final String addonID;

  final String addonURL;

  @override
  String toString() {
    return '{"key": "$key", "addonID": "$addonID", "addonURL": "$addonURL"}';
  }

  @override
  bool operator ==(covariant ExternalAddonWebviewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.addonID == addonID &&
        other.addonURL == addonURL;
  }

  @override
  int get hashCode {
    return key.hashCode ^ addonID.hashCode ^ addonURL.hashCode;
  }
}

class AddonContactsViewArguments {
  const AddonContactsViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonContactsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class RenewMembershipViewArguments {
  const RenewMembershipViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant RenewMembershipViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class GenericWebviewViewArguments {
  const GenericWebviewViewArguments({
    this.key,
    required this.url,
    required this.title,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String url;

  final String title;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "url": "$url", "title": "$title", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant GenericWebviewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.url == url &&
        other.title == title &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^
        url.hashCode ^
        title.hashCode ^
        previousPageTitle.hashCode;
  }
}

class AddonTestAssistantViewArguments {
  const AddonTestAssistantViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonTestAssistantViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonAreaDocumentsViewArguments {
  const AddonAreaDocumentsViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonAreaDocumentsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class EventsMapViewArguments {
  const EventsMapViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant EventsMapViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddEventViewArguments {
  const AddEventViewArguments({
    this.key,
    this.event,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final _i36.EventModel? event;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "event": "$event", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddEventViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.event == event &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ event.hashCode ^ previousPageTitle.hashCode;
  }
}

class MapPickerViewArguments {
  const MapPickerViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant MapPickerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class DocumentViewerViewArguments {
  const DocumentViewerViewArguments({
    this.key,
    required this.downlaodUrl,
    required this.title,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String downlaodUrl;

  final String title;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "downlaodUrl": "$downlaodUrl", "title": "$title", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant DocumentViewerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.downlaodUrl == downlaodUrl &&
        other.title == title &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^
        downlaodUrl.hashCode ^
        title.hashCode ^
        previousPageTitle.hashCode;
  }
}

class EventCalendarViewArguments {
  const EventCalendarViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant EventCalendarViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class CalendarLinkerViewArguments {
  const CalendarLinkerViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant CalendarLinkerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class EventShowcaseViewArguments {
  const EventShowcaseViewArguments({
    this.key,
    required this.previousPageTitle,
    required this.event,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  final _i36.EventModel event;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle", "event": "$event"}';
  }

  @override
  bool operator ==(covariant EventShowcaseViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.previousPageTitle == previousPageTitle &&
        other.event == event;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode ^ event.hashCode;
  }
}

class AddEventScheduleListViewArguments {
  const AddEventScheduleListViewArguments({
    this.key,
    required this.eventSchedules,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final List<_i37.EventScheduleModel> eventSchedules;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "eventSchedules": "$eventSchedules", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddEventScheduleListViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.eventSchedules == eventSchedules &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ eventSchedules.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddScheduleViewArguments {
  const AddScheduleViewArguments({
    this.key,
    this.event,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final _i37.EventScheduleModel? event;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "event": "$event", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddScheduleViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.event == event &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ event.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonDealsViewArguments {
  const AddonDealsViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonDealsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonDealsDetailsViewArguments {
  const AddonDealsDetailsViewArguments({
    this.key,
    required this.deal,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final _i38.DealModel deal;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "deal": "$deal", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonDealsDetailsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.deal == deal &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ deal.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonDealsAddViewArguments {
  const AddonDealsAddViewArguments({
    this.key,
    this.deal,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final _i38.DealModel? deal;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "deal": "$deal", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonDealsAddViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.deal == deal &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ deal.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonStampViewArguments {
  const AddonStampViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonStampViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class NotificationManagerViewArguments {
  const NotificationManagerViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant NotificationManagerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class PaymentMethodManagerViewArguments {
  const PaymentMethodManagerViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant PaymentMethodManagerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class MakeDonationViewArguments {
  const MakeDonationViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant MakeDonationViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class ReceiptsViewArguments {
  const ReceiptsViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant ReceiptsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonBoutiqueViewArguments {
  const AddonBoutiqueViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonBoutiqueViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonBoutiqueProductViewArguments {
  const AddonBoutiqueProductViewArguments({
    this.key,
    required this.product,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final _i39.BoutiqueModel product;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "product": "$product", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonBoutiqueProductViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.product == product &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ product.hashCode ^ previousPageTitle.hashCode;
  }
}

class AddonAreaDocumentsPreviewViewArguments {
  const AddonAreaDocumentsPreviewViewArguments({
    this.key,
    required this.document,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final _i40.DocumentModel document;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "document": "$document", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant AddonAreaDocumentsPreviewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key &&
        other.document == document &&
        other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ document.hashCode ^ previousPageTitle.hashCode;
  }
}

class NotificationViewViewArguments {
  const NotificationViewViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant NotificationViewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class DevicesViewArguments {
  const DevicesViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant DevicesViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class LocationListPickerViewArguments {
  const LocationListPickerViewArguments({
    this.key,
    required this.previousPageTitle,
  });

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant LocationListPickerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

class TicketsViewArguments {
  const TicketsViewArguments({this.key, required this.previousPageTitle});

  final _i35.Key? key;

  final String previousPageTitle;

  @override
  String toString() {
    return '{"key": "$key", "previousPageTitle": "$previousPageTitle"}';
  }

  @override
  bool operator ==(covariant TicketsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.previousPageTitle == previousPageTitle;
  }

  @override
  int get hashCode {
    return key.hashCode ^ previousPageTitle.hashCode;
  }
}

extension NavigatorStateExtension on _i41.NavigationService {
  Future<dynamic> navigateToLoginView({
    _i35.Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.loginView,
      arguments: LoginViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToStartupView({
    _i35.Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.startupView,
      arguments: StartupViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToHomeView({
    _i35.Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.homeView,
      arguments: HomeViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToExternalAddonWebviewView({
    _i35.Key? key,
    required String addonID,
    required String addonURL,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.externalAddonWebviewView,
      arguments: ExternalAddonWebviewViewArguments(
        key: key,
        addonID: addonID,
        addonURL: addonURL,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonContactsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonContactsView,
      arguments: AddonContactsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToRenewMembershipView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.renewMembershipView,
      arguments: RenewMembershipViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToGenericWebviewView({
    _i35.Key? key,
    required String url,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.genericWebviewView,
      arguments: GenericWebviewViewArguments(
        key: key,
        url: url,
        title: title,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonTestAssistantView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonTestAssistantView,
      arguments: AddonTestAssistantViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonAreaDocumentsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonAreaDocumentsView,
      arguments: AddonAreaDocumentsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToEventsMapView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.eventsMapView,
      arguments: EventsMapViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddEventView({
    _i35.Key? key,
    _i36.EventModel? event,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addEventView,
      arguments: AddEventViewArguments(
        key: key,
        event: event,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToMapPickerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.mapPickerView,
      arguments: MapPickerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToDocumentViewerView({
    _i35.Key? key,
    required String downlaodUrl,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.documentViewerView,
      arguments: DocumentViewerViewArguments(
        key: key,
        downlaodUrl: downlaodUrl,
        title: title,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToEventCalendarView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.eventCalendarView,
      arguments: EventCalendarViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToCalendarLinkerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.calendarLinkerView,
      arguments: CalendarLinkerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToEventShowcaseView({
    _i35.Key? key,
    required String previousPageTitle,
    required _i36.EventModel event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.eventShowcaseView,
      arguments: EventShowcaseViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
        event: event,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddEventScheduleListView({
    _i35.Key? key,
    required List<_i37.EventScheduleModel> eventSchedules,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addEventScheduleListView,
      arguments: AddEventScheduleListViewArguments(
        key: key,
        eventSchedules: eventSchedules,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddScheduleView({
    _i35.Key? key,
    _i37.EventScheduleModel? event,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addScheduleView,
      arguments: AddScheduleViewArguments(
        key: key,
        event: event,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonDealsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonDealsView,
      arguments: AddonDealsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonDealsDetailsView({
    _i35.Key? key,
    required _i38.DealModel deal,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonDealsDetailsView,
      arguments: AddonDealsDetailsViewArguments(
        key: key,
        deal: deal,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonDealsAddView({
    _i35.Key? key,
    _i38.DealModel? deal,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonDealsAddView,
      arguments: AddonDealsAddViewArguments(
        key: key,
        deal: deal,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonStampView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonStampView,
      arguments: AddonStampViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToNotificationManagerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.notificationManagerView,
      arguments: NotificationManagerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToPaymentMethodManagerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.paymentMethodManagerView,
      arguments: PaymentMethodManagerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToMakeDonationView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.makeDonationView,
      arguments: MakeDonationViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToReceiptsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.receiptsView,
      arguments: ReceiptsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonBoutiqueView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonBoutiqueView,
      arguments: AddonBoutiqueViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonBoutiqueProductView({
    _i35.Key? key,
    required _i39.BoutiqueModel product,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonBoutiqueProductView,
      arguments: AddonBoutiqueProductViewArguments(
        key: key,
        product: product,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToAddonAreaDocumentsPreviewView({
    _i35.Key? key,
    required _i40.DocumentModel document,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.addonAreaDocumentsPreviewView,
      arguments: AddonAreaDocumentsPreviewViewArguments(
        key: key,
        document: document,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToNotificationViewView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.notificationViewView,
      arguments: NotificationViewViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToDevicesView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.devicesView,
      arguments: DevicesViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToLocationListPickerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.locationListPickerView,
      arguments: LocationListPickerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> navigateToTicketsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(
      Routes.ticketsView,
      arguments: TicketsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithLoginView({
    _i35.Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.loginView,
      arguments: LoginViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithStartupView({
    _i35.Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.startupView,
      arguments: StartupViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithHomeView({
    _i35.Key? key,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.homeView,
      arguments: HomeViewArguments(key: key),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithExternalAddonWebviewView({
    _i35.Key? key,
    required String addonID,
    required String addonURL,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.externalAddonWebviewView,
      arguments: ExternalAddonWebviewViewArguments(
        key: key,
        addonID: addonID,
        addonURL: addonURL,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonContactsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonContactsView,
      arguments: AddonContactsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithRenewMembershipView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.renewMembershipView,
      arguments: RenewMembershipViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithGenericWebviewView({
    _i35.Key? key,
    required String url,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.genericWebviewView,
      arguments: GenericWebviewViewArguments(
        key: key,
        url: url,
        title: title,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonTestAssistantView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonTestAssistantView,
      arguments: AddonTestAssistantViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonAreaDocumentsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonAreaDocumentsView,
      arguments: AddonAreaDocumentsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithEventsMapView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.eventsMapView,
      arguments: EventsMapViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddEventView({
    _i35.Key? key,
    _i36.EventModel? event,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addEventView,
      arguments: AddEventViewArguments(
        key: key,
        event: event,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithMapPickerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.mapPickerView,
      arguments: MapPickerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithDocumentViewerView({
    _i35.Key? key,
    required String downlaodUrl,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.documentViewerView,
      arguments: DocumentViewerViewArguments(
        key: key,
        downlaodUrl: downlaodUrl,
        title: title,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithEventCalendarView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.eventCalendarView,
      arguments: EventCalendarViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithCalendarLinkerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.calendarLinkerView,
      arguments: CalendarLinkerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithEventShowcaseView({
    _i35.Key? key,
    required String previousPageTitle,
    required _i36.EventModel event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.eventShowcaseView,
      arguments: EventShowcaseViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
        event: event,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddEventScheduleListView({
    _i35.Key? key,
    required List<_i37.EventScheduleModel> eventSchedules,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addEventScheduleListView,
      arguments: AddEventScheduleListViewArguments(
        key: key,
        eventSchedules: eventSchedules,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddScheduleView({
    _i35.Key? key,
    _i37.EventScheduleModel? event,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addScheduleView,
      arguments: AddScheduleViewArguments(
        key: key,
        event: event,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonDealsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonDealsView,
      arguments: AddonDealsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonDealsDetailsView({
    _i35.Key? key,
    required _i38.DealModel deal,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonDealsDetailsView,
      arguments: AddonDealsDetailsViewArguments(
        key: key,
        deal: deal,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonDealsAddView({
    _i35.Key? key,
    _i38.DealModel? deal,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonDealsAddView,
      arguments: AddonDealsAddViewArguments(
        key: key,
        deal: deal,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonStampView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonStampView,
      arguments: AddonStampViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithNotificationManagerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.notificationManagerView,
      arguments: NotificationManagerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithPaymentMethodManagerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.paymentMethodManagerView,
      arguments: PaymentMethodManagerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithMakeDonationView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.makeDonationView,
      arguments: MakeDonationViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithReceiptsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.receiptsView,
      arguments: ReceiptsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonBoutiqueView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonBoutiqueView,
      arguments: AddonBoutiqueViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonBoutiqueProductView({
    _i35.Key? key,
    required _i39.BoutiqueModel product,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonBoutiqueProductView,
      arguments: AddonBoutiqueProductViewArguments(
        key: key,
        product: product,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithAddonAreaDocumentsPreviewView({
    _i35.Key? key,
    required _i40.DocumentModel document,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.addonAreaDocumentsPreviewView,
      arguments: AddonAreaDocumentsPreviewViewArguments(
        key: key,
        document: document,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithNotificationViewView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.notificationViewView,
      arguments: NotificationViewViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithDevicesView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.devicesView,
      arguments: DevicesViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithLocationListPickerView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.locationListPickerView,
      arguments: LocationListPickerViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }

  Future<dynamic> replaceWithTicketsView({
    _i35.Key? key,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(
      Routes.ticketsView,
      arguments: TicketsViewArguments(
        key: key,
        previousPageTitle: previousPageTitle,
      ),
      id: routerId,
      preventDuplicates: preventDuplicates,
      parameters: parameters,
      transition: transition,
    );
  }
}
