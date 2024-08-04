import 'dart:ui';

import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:webview_cookie_manager/webview_cookie_manager.dart';
import 'package:webview_flutter/webview_flutter.dart';

class GenericWebviewViewModel extends MasterModel {
  WebviewCookieManager cookieManager = WebviewCookieManager();
  WebViewController? controller;
  final String url;

  GenericWebviewViewModel({required this.url}) {
    ScraperApi().getCookieJar().then((cookieMn) {
      cookieMn.cookieJar.loadForRequest(Uri.parse(url)).then((cookies) async {
        await cookieManager.setCookies(cookies);
        controller = WebViewController()
          ..setJavaScriptMode(JavaScriptMode.unrestricted)
          ..setBackgroundColor(const Color(0x00000000))
          ..setNavigationDelegate(
            NavigationDelegate(
              onProgress: (int progress) {},
              onPageStarted: (String url) {},
              onPageFinished: (String url) {},
              onHttpError: (HttpResponseError error) {},
              onWebResourceError: (WebResourceError error) {},
              onNavigationRequest: (NavigationRequest request) {
                return NavigationDecision.navigate;
              },
            ),
          )
          ..loadRequest(Uri.parse(url));
        rebuildUi();
      });
    });
  }
}
