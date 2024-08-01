// GENERATED CODE - DO NOT MODIFY BY HAND

// **************************************************************************
// StackedNavigatorGenerator
// **************************************************************************

// ignore_for_file: no_leading_underscores_for_library_prefixes
import 'package:flutter/material.dart' as _i12;
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/views/addon_area_documents/addon_area_documents_view.dart'
    as _i11;
import 'package:mensa_italia_app/ui/views/addon_contacts/addon_contacts_view.dart'
    as _i7;
import 'package:mensa_italia_app/ui/views/addon_test_assistant/addon_test_assistant_view.dart'
    as _i10;
import 'package:mensa_italia_app/ui/views/external_addon_webview/external_addon_webview_view.dart'
    as _i6;
import 'package:mensa_italia_app/ui/views/home/home_view.dart' as _i5;
import 'package:mensa_italia_app/ui/views/login/login_view.dart' as _i2;
import 'package:mensa_italia_app/ui/views/renew_membership/renew_membership_view.dart'
    as _i8;
import 'package:mensa_italia_app/ui/views/renew_membership_webview/renew_membership_webview_view.dart'
    as _i9;
import 'package:mensa_italia_app/ui/views/sigs_list/sigs_list_view.dart' as _i4;
import 'package:mensa_italia_app/ui/views/startup/startup_view.dart' as _i3;
import 'package:stacked/stacked.dart' as _i1;
import 'package:stacked_services/stacked_services.dart' as _i13;

class Routes {
  static const loginView = '/login-view';

  static const startupView = '/startup-view';

  static const sigsListView = '/sigs-list-view';

  static const homeView = '/home-view';

  static const externalAddonWebviewView = '/external-addon-webview-view';

  static const addonContactsView = '/addon-contacts-view';

  static const renewMembershipView = '/renew-membership-view';

  static const renewMembershipWebviewView = '/renew-membership-webview-view';

  static const addonTestAssistantView = '/addon-test-assistant-view';

  static const addonAreaDocumentsView = '/addon-area-documents-view';

  static const all = <String>{
    loginView,
    startupView,
    sigsListView,
    homeView,
    externalAddonWebviewView,
    addonContactsView,
    renewMembershipView,
    renewMembershipWebviewView,
    addonTestAssistantView,
    addonAreaDocumentsView,
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
      Routes.sigsListView,
      page: _i4.SigsListView,
    ),
    _i1.RouteDef(
      Routes.homeView,
      page: _i5.HomeView,
    ),
    _i1.RouteDef(
      Routes.externalAddonWebviewView,
      page: _i6.ExternalAddonWebviewView,
    ),
    _i1.RouteDef(
      Routes.addonContactsView,
      page: _i7.AddonContactsView,
    ),
    _i1.RouteDef(
      Routes.renewMembershipView,
      page: _i8.RenewMembershipView,
    ),
    _i1.RouteDef(
      Routes.renewMembershipWebviewView,
      page: _i9.RenewMembershipWebviewView,
    ),
    _i1.RouteDef(
      Routes.addonTestAssistantView,
      page: _i10.AddonTestAssistantView,
    ),
    _i1.RouteDef(
      Routes.addonAreaDocumentsView,
      page: _i11.AddonAreaDocumentsView,
    ),
  ];

  final _pagesMap = <Type, _i1.StackedRouteFactory>{
    _i2.LoginView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i2.LoginView(),
        settings: data,
      );
    },
    _i3.StartupView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i3.StartupView(),
        settings: data,
      );
    },
    _i4.SigsListView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i4.SigsListView(),
        settings: data,
      );
    },
    _i5.HomeView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i5.HomeView(),
        settings: data,
      );
    },
    _i6.ExternalAddonWebviewView: (data) {
      final args =
          data.getArgs<ExternalAddonWebviewViewArguments>(nullOk: false);
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i6.ExternalAddonWebviewView(key: args.key, addonID: args.addonID),
        settings: data,
      );
    },
    _i7.AddonContactsView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i7.AddonContactsView(),
        settings: data,
      );
    },
    _i8.RenewMembershipView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i8.RenewMembershipView(),
        settings: data,
      );
    },
    _i9.RenewMembershipWebviewView: (data) {
      final args =
          data.getArgs<RenewMembershipWebviewViewArguments>(nullOk: false);
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) =>
            _i9.RenewMembershipWebviewView(key: args.key, url: args.url),
        settings: data,
      );
    },
    _i10.AddonTestAssistantView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i10.AddonTestAssistantView(),
        settings: data,
      );
    },
    _i11.AddonAreaDocumentsView: (data) {
      return _i12.MaterialPageRoute<dynamic>(
        builder: (context) => const _i11.AddonAreaDocumentsView(),
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

  final _i12.Key? key;

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

class RenewMembershipWebviewViewArguments {
  const RenewMembershipWebviewViewArguments({
    this.key,
    required this.url,
  });

  final _i12.Key? key;

  final String url;

  @override
  String toString() {
    return '{"key": "$key", "url": "$url"}';
  }

  @override
  bool operator ==(covariant RenewMembershipWebviewViewArguments other) {
    if (identical(this, other)) return true;
    return other.key == key && other.url == url;
  }

  @override
  int get hashCode {
    return key.hashCode ^ url.hashCode;
  }
}

extension NavigatorStateExtension on _i13.NavigationService {
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

  Future<dynamic> navigateToSigsListView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return navigateTo<dynamic>(Routes.sigsListView,
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
    _i12.Key? key,
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

  Future<dynamic> navigateToRenewMembershipWebviewView({
    _i12.Key? key,
    required String url,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return navigateTo<dynamic>(Routes.renewMembershipWebviewView,
        arguments: RenewMembershipWebviewViewArguments(key: key, url: url),
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

  Future<dynamic> replaceWithSigsListView([
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  ]) async {
    return replaceWith<dynamic>(Routes.sigsListView,
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
    _i12.Key? key,
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

  Future<dynamic> replaceWithRenewMembershipWebviewView({
    _i12.Key? key,
    required String url,
    int? routerId,
    bool preventDuplicates = true,
    Map<String, String>? parameters,
    Widget Function(BuildContext, Animation<double>, Animation<double>, Widget)?
        transition,
  }) async {
    return replaceWith<dynamic>(Routes.renewMembershipWebviewView,
        arguments: RenewMembershipWebviewViewArguments(key: key, url: url),
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
}
