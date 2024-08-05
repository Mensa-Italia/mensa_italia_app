// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedNavigatorGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart' as _i15;
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/event.dart' as _i16;
import 'package:mensa_italia_app/ui/views/add_event/add_event_view.dart'
    as _i12;
import 'package:mensa_italia_app/ui/views/addon_area_documents/addon_area_documents_view.dart'
    as _i10;
import 'package:mensa_italia_app/ui/views/addon_contacts/addon_contacts_view.dart'
    as _i6;
import 'package:mensa_italia_app/ui/views/addon_test_assistant/addon_test_assistant_view.dart'
    as _i9;
import 'package:mensa_italia_app/ui/views/document_viewer/document_viewer_view.dart'
    as _i14;
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
import 'package:mensa_italia_app/ui/views/renew_membership/renew_membership_view.dart'
    as _i7;
import 'package:mensa_italia_app/ui/views/startup/startup_view.dart' as _i3;
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i17;

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
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.LoginView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i2.LoginView(),
        settings: data,
      );
    },
    _i3.StartupView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i3.StartupView(),
        settings: data,
      );
    },
    _i4.HomeView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i4.HomeView(),
        settings: data,
      );
    },
    _i5.ExternalAddonWebviewView: (data) {
      final args =
          data.getArgs<ExternalAddonWebviewViewArguments>(nullOk: false);
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i5.ExternalAddonWebviewView(key: args.key, addonID: args.addonID),
        settings: data,
      );
    },
    _i6.AddonContactsView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i6.AddonContactsView(),
        settings: data,
      );
    },
    _i7.RenewMembershipView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i7.RenewMembershipView(),
        settings: data,
      );
    },
    _i8.GenericWebviewView: (data) {
      final args = data.getArgs<GenericWebviewViewArguments>(nullOk: false);
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => _i8.GenericWebviewView(
            key: args.key,
            url: args.url,
            title: args.title,
            previousPageTitle: args.previousPageTitle),
        settings: data,
      );
    },
    _i9.AddonTestAssistantView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i9.AddonTestAssistantView(),
        settings: data,
      );
    },
    _i10.AddonAreaDocumentsView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i10.AddonAreaDocumentsView(),
        settings: data,
      );
    },
    _i11.EventsMapView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i11.EventsMapView(),
        settings: data,
      );
    },
    _i12.AddEventView: (data) {
      final args = data.getArgs<AddEventViewArguments>(
        orElse: () => const AddEventViewArguments(),
      );
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i12.AddEventView(key: args.key, event: args.event),
        settings: data,
      );
    },
    _i13.MapPickerView: (data) {
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => const _i13.MapPickerView(),
        settings: data,
      );
    },
    _i14.DocumentViewerView: (data) {
      final args = data.getArgs<DocumentViewerViewArguments>(nullOk: false);
      return _i15.MaterialPageRoute<dynamic>(
        builder: (context) => _i14.DocumentViewerView(
            key: args.key, downlaodUrl: args.downlaodUrl),
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
  });

  final _i15.Key? key;

  final String addonID;

  @override
  String toString() {
    return '{"key": "$key", "addonID": "$addonID"}';
  }

  @override
  bool operator ==(covariant ExternalAddonWebviewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.addonID == addonID;
  }

  @override
  int get hashCode {
    return key.hashCode ^ addonID.hashCode;
  }
}

class GenericWebviewViewArguments {
  const GenericWebviewViewArguments({
    this.key,
    required this.url,
    required this.title,
    required this.previousPageTitle,
  });

  final _i15.Key? key;

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

  final _i15.Key? key;

  final _i16.EventModel? event;

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
  });

  final _i15.Key? key;

  final String downlaodUrl;

  @override
  String toString() {
    return '{"key": "$key", "downlaodUrl": "$downlaodUrl"}';
  }

  @override
  bool operator ==(covariant DocumentViewerViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.downlaodUrl == downlaodUrl;
  }

  @override
  int get hashCode {
    return key.hashCode ^ downlaodUrl.hashCode;
  }
}

extension NavigatorStateExtension on _i17.NavigationService {
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
    _i15.Key? key,
    required String addonID,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.externalAddonWebviewView,
        arguments:
            ExternalAddonWebviewViewArguments(key: key, addonID: addonID),
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
    _i15.Key? key,
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
    _i15.Key? key,
    _i16.EventModel? event,
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
    _i15.Key? key,
    required String downlaodUrl,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.documentViewerView,
        arguments:
            DocumentViewerViewArguments(key: key, downlaodUrl: downlaodUrl),
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
    _i15.Key? key,
    required String addonID,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.externalAddonWebviewView,
        arguments:
            ExternalAddonWebviewViewArguments(key: key, addonID: addonID),
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
    _i15.Key? key,
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
    _i15.Key? key,
    _i16.EventModel? event,
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
    _i15.Key? key,
    required String downlaodUrl,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.documentViewerView,
        arguments:
            DocumentViewerViewArguments(key: key, downlaodUrl: downlaodUrl),
        id: routerId,
        preventDuplicates: preventDuplicates,
        parameters: parameters,
        transition: transition);
  }
}
