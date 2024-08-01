import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:webview_flutter/webview_flutter.dart';

class ExternalAddonWebviewViewModel extends MasterModel {
  WebViewController? controller;
  bool _canGoBack = true;

  ExternalAddonWebviewViewModel(String addonId) {
    Api().getAddonAccessData(addonId).then(
      (value) {
        Uri url = Uri.parse('https://mensa-italia.github.io/mensa_app_addon/');
        url = url.replace(queryParameters: value);
        controller = WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..setBackgroundColor(const Color(0x00000000))
          ..setNavigationDelegate(
            NavigationDelegate(
              onProgress: (int progress) {},
              onPageStarted: (String url) {},
              onPageFinished: (String url) {
                controller!.canGoBack().then((value) {
                  _canGoBack = !value;
                  rebuildUi();
                });
              },
              onHttpError: (HttpResponseError error) {},
              onWebResourceError: (WebResourceError error) {},
              onNavigationRequest: (NavigationRequest request) {
                if (request.url.contains('svc.mensa.it/goback')) {
                  NavigationService().back();
                  return NavigationDecision.prevent;
                }
                return NavigationDecision.navigate;
              },
            ),
          )
          ..loadRequest(url);
        rebuildUi();
      },
    );
  }

  bool willPopCallback() {
    if (controller == null) return true;
    return _canGoBack;
  }

  void onPopInvoked(bool pop) {
    if (controller == null) return;
    controller!.goBack();
  }
}
