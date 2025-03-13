// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedNavigatorGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart' as _i33;
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/boutique.dart' as _i37;
import 'package:mensa_italia_app/model/deal.dart' as _i36;
import 'package:mensa_italia_app/model/document.dart' as _i38;
import 'package:mensa_italia_app/model/event.dart' as _i34;
import 'package:mensa_italia_app/model/event_schedule.dart' as _i35;
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
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i39;

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
  };
}

class StackedRouter extends _i1.RouterBase {
  final _routes = <_i1.RouteDef>[
    _i1.RouteDef(
      Routes.loginView,
      page: _i2.LoginView,
    ),
    _i1.RouteDef(
      Routes.startupView,
      page: _i3.StartupView,
    ),
    _i1.RouteDef(
      Routes.homeView,
      page: _i4.HomeView,
    ),
    _i1.RouteDef(
      Routes.externalAddonWebviewView,
      page: _i5.ExternalAddonWebviewView,
    ),
    _i1.RouteDef(
      Routes.addonContactsView,
      page: _i6.AddonContactsView,
    ),
    _i1.RouteDef(
      Routes.renewMembershipView,
      page: _i7.RenewMembershipView,
    ),
    _i1.RouteDef(
      Routes.genericWebviewView,
      page: _i8.GenericWebviewView,
    ),
    _i1.RouteDef(
      Routes.addonTestAssistantView,
      page: _i9.AddonTestAssistantView,
    ),
    _i1.RouteDef(
      Routes.addonAreaDocumentsView,
      page: _i10.AddonAreaDocumentsView,
    ),
    _i1.RouteDef(
      Routes.eventsMapView,
      page: _i11.EventsMapView,
    ),
    _i1.RouteDef(
      Routes.addEventView,
      page: _i12.AddEventView,
    ),
    _i1.RouteDef(
      Routes.mapPickerView,
      page: _i13.MapPickerView,
    ),
    _i1.RouteDef(
      Routes.documentViewerView,
      page: _i14.DocumentViewerView,
    ),
    _i1.RouteDef(
      Routes.eventCalendarView,
      page: _i15.EventCalendarView,
    ),
    _i1.RouteDef(
      Routes.calendarLinkerView,
      page: _i16.CalendarLinkerView,
    ),
    _i1.RouteDef(
      Routes.eventShowcaseView,
      page: _i17.EventShowcaseView,
    ),
    _i1.RouteDef(
      Routes.addEventScheduleListView,
      page: _i18.AddEventScheduleListView,
    ),
    _i1.RouteDef(
      Routes.addScheduleView,
      page: _i19.AddScheduleView,
    ),
    _i1.RouteDef(
      Routes.addonDealsView,
      page: _i20.AddonDealsView,
    ),
    _i1.RouteDef(
      Routes.addonDealsDetailsView,
      page: _i21.AddonDealsDetailsView,
    ),
    _i1.RouteDef(
      Routes.addonDealsAddView,
      page: _i22.AddonDealsAddView,
    ),
    _i1.RouteDef(
      Routes.addonStampView,
      page: _i23.AddonStampView,
    ),
    _i1.RouteDef(
      Routes.notificationManagerView,
      page: _i24.NotificationManagerView,
    ),
    _i1.RouteDef(
      Routes.paymentMethodManagerView,
      page: _i25.PaymentMethodManagerView,
    ),
    _i1.RouteDef(
      Routes.makeDonationView,
      page: _i26.MakeDonationView,
    ),
    _i1.RouteDef(
      Routes.receiptsView,
      page: _i27.ReceiptsView,
    ),
    _i1.RouteDef(
      Routes.addonBoutiqueView,
      page: _i28.AddonBoutiqueView,
    ),
    _i1.RouteDef(
      Routes.addonBoutiqueProductView,
      page: _i29.AddonBoutiqueProductView,
    ),
    _i1.RouteDef(
      Routes.addonAreaDocumentsPreviewView,
      page: _i30.AddonAreaDocumentsPreviewView,
    ),
    _i1.RouteDef(
      Routes.notificationViewView,
      page: _i31.NotificationViewView,
    ),
    _i1.RouteDef(
      Routes.devicesView,
      page: _i32.DevicesView,
    ),
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.LoginView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i2.LoginView(),
        settings: data,
      );
    },
    _i3.StartupView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i3.StartupView(),
        settings: data,
      );
    },
    _i4.HomeView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i4.HomeView(),
        settings: data,
      );
    },
    _i5.ExternalAddonWebviewView: (data) {
      final args =
          data.getArgs<ExternalAddonWebviewViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => _i5.ExternalAddonWebviewView(
            key: args.key, addonID: args.addonID, addonURL: args.addonURL),
        settings: data,
      );
    },
    _i6.AddonContactsView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i6.AddonContactsView(),
        settings: data,
      );
    },
    _i7.RenewMembershipView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i7.RenewMembershipView(),
        settings: data,
      );
    },
    _i8.GenericWebviewView: (data) {
      final args = data.getArgs<GenericWebviewViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => _i8.GenericWebviewView(
            key: args.key,
            url: args.url,
            title: args.title,
            previousPageTitle: args.previousPageTitle),
        settings: data,
      );
    },
    _i9.AddonTestAssistantView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i9.AddonTestAssistantView(),
        settings: data,
      );
    },
    _i10.AddonAreaDocumentsView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i10.AddonAreaDocumentsView(),
        settings: data,
      );
    },
    _i11.EventsMapView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i11.EventsMapView(),
        settings: data,
      );
    },
    _i12.AddEventView: (data) {
      final args = data.getArgs<AddEventViewArguments>(
        orElse: () => const AddEventViewArguments(),
      );
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i12.AddEventView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i13.MapPickerView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i13.MapPickerView(),
        settings: data,
      );
    },
    _i14.DocumentViewerView: (data) {
      final args = data.getArgs<DocumentViewerViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => _i14.DocumentViewerView(
            key: args.key,
            downlaodUrl: args.downlaodUrl,
            title: args.title,
            previousPageTitle: args.previousPageTitle),
        settings: data,
      );
    },
    _i15.EventCalendarView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i15.EventCalendarView(),
        settings: data,
      );
    },
    _i16.CalendarLinkerView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i16.CalendarLinkerView(),
        settings: data,
      );
    },
    _i17.EventShowcaseView: (data) {
      final args = data.getArgs<EventShowcaseViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i17.EventShowcaseView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i18.AddEventScheduleListView: (data) {
      final args =
          data.getArgs<AddEventScheduleListViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => _i18.AddEventScheduleListView(
            key: args.key, eventSchedules: args.eventSchedules),
        settings: data,
      );
    },
    _i19.AddScheduleView: (data) {
      final args = data.getArgs<AddScheduleViewArguments>(
        orElse: () => const AddScheduleViewArguments(),
      );
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i19.AddScheduleView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i20.AddonDealsView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i20.AddonDealsView(),
        settings: data,
      );
    },
    _i21.AddonDealsDetailsView: (data) {
      final args = data.getArgs<AddonDealsDetailsViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i21.AddonDealsDetailsView(key: args.key, deal: args.deal),
        settings: data,
      );
    },
    _i22.AddonDealsAddView: (data) {
      final args = data.getArgs<AddonDealsAddViewArguments>(
        orElse: () => const AddonDealsAddViewArguments(),
      );
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i22.AddonDealsAddView(key: args.key, deal: args.deal),
        settings: data,
      );
    },
    _i23.AddonStampView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i23.AddonStampView(),
        settings: data,
      );
    },
    _i24.NotificationManagerView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i24.NotificationManagerView(),
        settings: data,
      );
    },
    _i25.PaymentMethodManagerView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i25.PaymentMethodManagerView(),
        settings: data,
      );
    },
    _i26.MakeDonationView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i26.MakeDonationView(),
        settings: data,
      );
    },
    _i27.ReceiptsView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i27.ReceiptsView(),
        settings: data,
      );
    },
    _i28.AddonBoutiqueView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i28.AddonBoutiqueView(),
        settings: data,
      );
    },
    _i29.AddonBoutiqueProductView: (data) {
      final args =
          data.getArgs<AddonBoutiqueProductViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i29.AddonBoutiqueProductView(key: args.key, product: args.product),
        settings: data,
      );
    },
    _i30.AddonAreaDocumentsPreviewView: (data) {
      final args =
          data.getArgs<AddonAreaDocumentsPreviewViewArguments>(nullOk: false);
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => _i30.AddonAreaDocumentsPreviewView(
            key: args.key, document: args.document),
        settings: data,
      );
    },
    _i31.NotificationViewView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i31.NotificationViewView(),
        settings: data,
      );
    },
    _i32.DevicesView: (data) {
      return _i33.MaterialPageRoute<dynamic>(
        builder: (context) => const _i32.DevicesView(),
        settings: data,
      );
    },
  };

  @override
  List<_i1.RouteDef> get routes => _routes;

  @override
  Map<Type, _i1.StackedRouteFactory> get pagesMap => _pagesMap;
}

class ExternalAddonWebviewViewArguments {
  const ExternalAddonWebviewViewArguments({
    this.key,
    required this.addonID,
    required this.addonURL,
  });

  final _i33.Key? key;

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

class GenericWebviewViewArguments {
  const GenericWebviewViewArguments({
    this.key,
    required this.url,
    required this.title,
    required this.previousPageTitle,
  });

  final _i33.Key? key;

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

class AddEventViewArguments {
  const AddEventViewArguments({
    this.key,
    this.event,
  });

  final _i33.Key? key;

  final _i34.EventModel? event;

  @override
  String toString() {
    return '{"key": "$key", "event": "$event"}';
  }

  @override
  bool operator ==(covariant AddEventViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.event == event;
  }

  @override
  int get hashCode {
    return key.hashCode ^ event.hashCode;
  }
}

class DocumentViewerViewArguments {
  const DocumentViewerViewArguments({
    this.key,
    required this.downlaodUrl,
    required this.title,
    required this.previousPageTitle,
  });

  final _i33.Key? key;

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

class EventShowcaseViewArguments {
  const EventShowcaseViewArguments({
    this.key,
    required this.event,
  });

  final _i33.Key? key;

  final _i34.EventModel event;

  @override
  String toString() {
    return '{"key": "$key", "event": "$event"}';
  }

  @override
  bool operator ==(covariant EventShowcaseViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.event == event;
  }

  @override
  int get hashCode {
    return key.hashCode ^ event.hashCode;
  }
}

class AddEventScheduleListViewArguments {
  const AddEventScheduleListViewArguments({
    this.key,
    required this.eventSchedules,
  });

  final _i33.Key? key;

  final List<_i35.EventScheduleModel> eventSchedules;

  @override
  String toString() {
    return '{"key": "$key", "eventSchedules": "$eventSchedules"}';
  }

  @override
  bool operator ==(covariant AddEventScheduleListViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.eventSchedules == eventSchedules;
  }

  @override
  int get hashCode {
    return key.hashCode ^ eventSchedules.hashCode;
  }
}

class AddScheduleViewArguments {
  const AddScheduleViewArguments({
    this.key,
    this.event,
  });

  final _i33.Key? key;

  final _i35.EventScheduleModel? event;

  @override
  String toString() {
    return '{"key": "$key", "event": "$event"}';
  }

  @override
  bool operator ==(covariant AddScheduleViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.event == event;
  }

  @override
  int get hashCode {
    return key.hashCode ^ event.hashCode;
  }
}

class AddonDealsDetailsViewArguments {
  const AddonDealsDetailsViewArguments({
    this.key,
    required this.deal,
  });

  final _i33.Key? key;

  final _i36.DealModel deal;

  @override
  String toString() {
    return '{"key": "$key", "deal": "$deal"}';
  }

  @override
  bool operator ==(covariant AddonDealsDetailsViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.deal == deal;
  }

  @override
  int get hashCode {
    return key.hashCode ^ deal.hashCode;
  }
}

class AddonDealsAddViewArguments {
  const AddonDealsAddViewArguments({
    this.key,
    this.deal,
  });

  final _i33.Key? key;

  final _i36.DealModel? deal;

  @override
  String toString() {
    return '{"key": "$key", "deal": "$deal"}';
  }

  @override
  bool operator ==(covariant AddonDealsAddViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.deal == deal;
  }

  @override
  int get hashCode {
    return key.hashCode ^ deal.hashCode;
  }
}

class AddonBoutiqueProductViewArguments {
  const AddonBoutiqueProductViewArguments({
    this.key,
    required this.product,
  });

  final _i33.Key? key;

  final _i37.BoutiqueModel product;

  @override
  String toString() {
    return '{"key": "$key", "product": "$product"}';
  }

  @override
  bool operator ==(covariant AddonBoutiqueProductViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.product == product;
  }

  @override
  int get hashCode {
    return key.hashCode ^ product.hashCode;
  }
}

class AddonAreaDocumentsPreviewViewArguments {
  const AddonAreaDocumentsPreviewViewArguments({
    this.key,
    required this.document,
  });

  final _i33.Key? key;

  final _i38.DocumentModel document;

  @override
  String toString() {
    return '{"key": "$key", "document": "$document"}';
  }

  @override
  bool operator ==(covariant AddonAreaDocumentsPreviewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.document == document;
  }

  @override
  int get hashCode {
    return key.hashCode ^ document.hashCode;
  }
}

extension NavigatorStateExtension on _i39.NavigationService {
  Future<dynamic> navigateToLoginView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.loginView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToStartupView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.startupView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToHomeView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.homeView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToExternalAddonWebviewView({
    _i33.Key? key,
    required String addonID,
    required String addonURL,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.externalAddonWebviewView,
        arguments: ExternalAddonWebviewViewArguments(
            key: key, addonID: addonID, addonURL: addonURL),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonContactsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.addonContactsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToRenewMembershipView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.renewMembershipView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToGenericWebviewView({
    _i33.Key? key,
    required String url,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.genericWebviewView,
        arguments: GenericWebviewViewArguments(
            key: key,
            url: url,
            title: title,
            previousPageTitle: previousPageTitle),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonTestAssistantView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.addonTestAssistantView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonAreaDocumentsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.addonAreaDocumentsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToEventsMapView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.eventsMapView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddEventView({
    _i33.Key? key,
    _i34.EventModel? event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addEventView,
        arguments: AddEventViewArguments(key: key, event: event),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToMapPickerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.mapPickerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToDocumentViewerView({
    _i33.Key? key,
    required String downlaodUrl,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.documentViewerView,
        arguments: DocumentViewerViewArguments(
            key: key,
            downlaodUrl: downlaodUrl,
            title: title,
            previousPageTitle: previousPageTitle),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToEventCalendarView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.eventCalendarView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToCalendarLinkerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.calendarLinkerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToEventShowcaseView({
    _i33.Key? key,
    required _i34.EventModel event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.eventShowcaseView,
        arguments: EventShowcaseViewArguments(key: key, event: event),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddEventScheduleListView({
    _i33.Key? key,
    required List<_i35.EventScheduleModel> eventSchedules,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addEventScheduleListView,
        arguments: AddEventScheduleListViewArguments(
            key: key, eventSchedules: eventSchedules),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddScheduleView({
    _i33.Key? key,
    _i35.EventScheduleModel? event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addScheduleView,
        arguments: AddScheduleViewArguments(key: key, event: event),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonDealsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.addonDealsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonDealsDetailsView({
    _i33.Key? key,
    required _i36.DealModel deal,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addonDealsDetailsView,
        arguments: AddonDealsDetailsViewArguments(key: key, deal: deal),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonDealsAddView({
    _i33.Key? key,
    _i36.DealModel? deal,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addonDealsAddView,
        arguments: AddonDealsAddViewArguments(key: key, deal: deal),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonStampView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.addonStampView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToNotificationManagerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.notificationManagerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToPaymentMethodManagerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.paymentMethodManagerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToMakeDonationView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.makeDonationView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToReceiptsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.receiptsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonBoutiqueView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.addonBoutiqueView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonBoutiqueProductView({
    _i33.Key? key,
    required _i37.BoutiqueModel product,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addonBoutiqueProductView,
        arguments:
            AddonBoutiqueProductViewArguments(key: key, product: product),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToAddonAreaDocumentsPreviewView({
    _i33.Key? key,
    required _i38.DocumentModel document,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.addonAreaDocumentsPreviewView,
        arguments: AddonAreaDocumentsPreviewViewArguments(
            key: key, document: document),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToNotificationViewView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.notificationViewView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> navigateToDevicesView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.devicesView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithLoginView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.loginView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithStartupView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.startupView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithHomeView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.homeView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithExternalAddonWebviewView({
    _i33.Key? key,
    required String addonID,
    required String addonURL,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.externalAddonWebviewView,
        arguments: ExternalAddonWebviewViewArguments(
            key: key, addonID: addonID, addonURL: addonURL),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonContactsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.addonContactsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithRenewMembershipView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.renewMembershipView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithGenericWebviewView({
    _i33.Key? key,
    required String url,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.genericWebviewView,
        arguments: GenericWebviewViewArguments(
            key: key,
            url: url,
            title: title,
            previousPageTitle: previousPageTitle),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonTestAssistantView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.addonTestAssistantView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonAreaDocumentsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.addonAreaDocumentsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithEventsMapView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.eventsMapView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddEventView({
    _i33.Key? key,
    _i34.EventModel? event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addEventView,
        arguments: AddEventViewArguments(key: key, event: event),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithMapPickerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.mapPickerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithDocumentViewerView({
    _i33.Key? key,
    required String downlaodUrl,
    required String title,
    required String previousPageTitle,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.documentViewerView,
        arguments: DocumentViewerViewArguments(
            key: key,
            downlaodUrl: downlaodUrl,
            title: title,
            previousPageTitle: previousPageTitle),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithEventCalendarView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.eventCalendarView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithCalendarLinkerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.calendarLinkerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithEventShowcaseView({
    _i33.Key? key,
    required _i34.EventModel event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.eventShowcaseView,
        arguments: EventShowcaseViewArguments(key: key, event: event),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddEventScheduleListView({
    _i33.Key? key,
    required List<_i35.EventScheduleModel> eventSchedules,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addEventScheduleListView,
        arguments: AddEventScheduleListViewArguments(
            key: key, eventSchedules: eventSchedules),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddScheduleView({
    _i33.Key? key,
    _i35.EventScheduleModel? event,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addScheduleView,
        arguments: AddScheduleViewArguments(key: key, event: event),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonDealsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.addonDealsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonDealsDetailsView({
    _i33.Key? key,
    required _i36.DealModel deal,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addonDealsDetailsView,
        arguments: AddonDealsDetailsViewArguments(key: key, deal: deal),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonDealsAddView({
    _i33.Key? key,
    _i36.DealModel? deal,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addonDealsAddView,
        arguments: AddonDealsAddViewArguments(key: key, deal: deal),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonStampView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.addonStampView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithNotificationManagerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.notificationManagerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithPaymentMethodManagerView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.paymentMethodManagerView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithMakeDonationView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.makeDonationView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithReceiptsView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.receiptsView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonBoutiqueView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.addonBoutiqueView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonBoutiqueProductView({
    _i33.Key? key,
    required _i37.BoutiqueModel product,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addonBoutiqueProductView,
        arguments:
            AddonBoutiqueProductViewArguments(key: key, product: product),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithAddonAreaDocumentsPreviewView({
    _i33.Key? key,
    required _i38.DocumentModel document,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.addonAreaDocumentsPreviewView,
        arguments: AddonAreaDocumentsPreviewViewArguments(
            key: key, document: document),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithNotificationViewView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.notificationViewView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }

  Future<dynamic> replaceWithDevicesView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.devicesView,
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }
}
