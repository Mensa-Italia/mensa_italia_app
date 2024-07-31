import 'dart:ui';

import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';
import 'package:webview_flutter/webview_flutter.dart';

class RenewMembershipWebviewViewModel extends MasterModel {
  WebViewController? controller;
  final String url;

  RenewMembershipWebviewViewModel({required this.url}) {
    ScraperApi().getCookieJar().then((cookieManager) {
      cookieManager.cookieJar.loadForRequest(Uri.parse(url)).then((cookies) {
        for (var cookie in cookies) {
          if (cookie.name == "PHPSESSID") {
            WebViewCookieManager().setCookie(
              WebViewCookie(
                name: cookie.name,
                value: cookie.value,
                domain: cookie.domain ?? "",
              ),
            );
          }
        }

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
