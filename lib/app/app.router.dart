// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedNavigatorGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart' as _i26;
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/deal.dart' as _i29;
import 'package:mensa_italia_app/model/event.dart' as _i27;
import 'package:mensa_italia_app/model/event_schedule.dart' as _i28;
import 'package:mensa_italia_app/ui/views/add_event/add_event_view.dart'
    as _i12;
import 'package:mensa_italia_app/ui/views/add_event_schedule_list/add_event_schedule_list_view.dart'
    as _i18;
import 'package:mensa_italia_app/ui/views/add_schedule/add_schedule_view.dart'
    as _i19;
import 'package:mensa_italia_app/ui/views/addon_area_documents/addon_area_documents_view.dart'
    as _i10;
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
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_view.dart'
    as _i13;
import 'package:mensa_italia_app/ui/views/notification_manager/notification_manager_view.dart'
    as _i24;
import 'package:mensa_italia_app/ui/views/payment_method_manager/payment_method_manager_view.dart'
    as _i25;
import 'package:mensa_italia_app/ui/views/renew_membership/renew_membership_view.dart'
    as _i7;
import 'package:mensa_italia_app/ui/views/startup/startup_view.dart' as _i3;
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i30;

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
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.LoginView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i2.LoginView(),
        settings: data,
      );
    },
    _i3.StartupView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i3.StartupView(),
        settings: data,
      );
    },
    _i4.HomeView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i4.HomeView(),
        settings: data,
      );
    },
    _i5.ExternalAddonWebviewView: (data) {
      final args =
          data.getArgs<ExternalAddonWebviewViewArguments>(nullOk: false);
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => _i5.ExternalAddonWebviewView(
            key: args.key, addonID: args.addonID, addonURL: args.addonURL),
        settings: data,
      );
    },
    _i6.AddonContactsView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i6.AddonContactsView(),
        settings: data,
      );
    },
    _i7.RenewMembershipView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i7.RenewMembershipView(),
        settings: data,
      );
    },
    _i8.GenericWebviewView: (data) {
      final args = data.getArgs<GenericWebviewViewArguments>(nullOk: false);
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => _i8.GenericWebviewView(
            key: args.key,
            url: args.url,
            title: args.title,
            previousPageTitle: args.previousPageTitle),
        settings: data,
      );
    },
    _i9.AddonTestAssistantView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i9.AddonTestAssistantView(),
        settings: data,
      );
    },
    _i10.AddonAreaDocumentsView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i10.AddonAreaDocumentsView(),
        settings: data,
      );
    },
    _i11.EventsMapView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i11.EventsMapView(),
        settings: data,
      );
    },
    _i12.AddEventView: (data) {
      final args = data.getArgs<AddEventViewArguments>(
        orElse: () => const AddEventViewArguments(),
      );
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i12.AddEventView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i13.MapPickerView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i13.MapPickerView(),
        settings: data,
      );
    },
    _i14.DocumentViewerView: (data) {
      final args = data.getArgs<DocumentViewerViewArguments>(nullOk: false);
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => _i14.DocumentViewerView(
            key: args.key,
            downlaodUrl: args.downlaodUrl,
            title: args.title,
            previousPageTitle: args.previousPageTitle),
        settings: data,
      );
    },
    _i15.EventCalendarView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i15.EventCalendarView(),
        settings: data,
      );
    },
    _i16.CalendarLinkerView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i16.CalendarLinkerView(),
        settings: data,
      );
    },
    _i17.EventShowcaseView: (data) {
      final args = data.getArgs<EventShowcaseViewArguments>(nullOk: false);
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i17.EventShowcaseView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i18.AddEventScheduleListView: (data) {
      final args =
          data.getArgs<AddEventScheduleListViewArguments>(nullOk: false);
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => _i18.AddEventScheduleListView(
            key: args.key, eventSchedules: args.eventSchedules),
        settings: data,
      );
    },
    _i19.AddScheduleView: (data) {
      final args = data.getArgs<AddScheduleViewArguments>(
        orElse: () => const AddScheduleViewArguments(),
      );
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i19.AddScheduleView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i20.AddonDealsView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i20.AddonDealsView(),
        settings: data,
      );
    },
    _i21.AddonDealsDetailsView: (data) {
      final args = data.getArgs<AddonDealsDetailsViewArguments>(nullOk: false);
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i21.AddonDealsDetailsView(key: args.key, deal: args.deal),
        settings: data,
      );
    },
    _i22.AddonDealsAddView: (data) {
      final args = data.getArgs<AddonDealsAddViewArguments>(
        orElse: () => const AddonDealsAddViewArguments(),
      );
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i22.AddonDealsAddView(key: args.key, deal: args.deal),
        settings: data,
      );
    },
    _i23.AddonStampView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i23.AddonStampView(),
        settings: data,
      );
    },
    _i24.NotificationManagerView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i24.NotificationManagerView(),
        settings: data,
      );
    },
    _i25.PaymentMethodManagerView: (data) {
      return _i26.MaterialPageRoute<dynamic>(
        builder: (context) => const _i25.PaymentMethodManagerView(),
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

  final _i26.Key? key;

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

  final _i26.Key? key;

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

  final _i26.Key? key;

  final _i27.EventModel? event;

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

  final _i26.Key? key;

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

  final _i26.Key? key;

  final _i27.EventModel event;

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

  final _i26.Key? key;

  final List<_i28.EventScheduleModel> eventSchedules;

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

  final _i26.Key? key;

  final _i28.EventScheduleModel? event;

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

  final _i26.Key? key;

  final _i29.DealModel deal;

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

  final _i26.Key? key;

  final _i29.DealModel? deal;

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

extension NavigatorStateExtension on _i30.NavigationService {
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
    _i26.Key? key,
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
    _i26.Key? key,
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
    _i26.Key? key,
    _i27.EventModel? event,
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
    _i26.Key? key,
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
    _i26.Key? key,
    required _i27.EventModel event,
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
    _i26.Key? key,
    required List<_i28.EventScheduleModel> eventSchedules,
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
    _i26.Key? key,
    _i28.EventScheduleModel? event,
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
    _i26.Key? key,
    required _i29.DealModel deal,
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
    _i26.Key? key,
    _i29.DealModel? deal,
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
    _i26.Key? key,
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
    _i26.Key? key,
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
    _i26.Key? key,
    _i27.EventModel? event,
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
    _i26.Key? key,
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
    _i26.Key? key,
    required _i27.EventModel event,
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
    _i26.Key? key,
    required List<_i28.EventScheduleModel> eventSchedules,
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
    _i26.Key? key,
    _i28.EventScheduleModel? event,
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
    _i26.Key? key,
    required _i29.DealModel deal,
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
    _i26.Key? key,
    _i29.DealModel? deal,
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
}
