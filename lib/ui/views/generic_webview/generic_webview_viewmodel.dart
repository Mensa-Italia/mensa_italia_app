import 'dart:ui';

import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:webview_flutter/webview_flutter.dart';

class GenericWebviewViewModel extends MasterModel {
  WebViewController? controller;
  final String url;

  GenericWebviewViewModel({required this.url}) {
    ScraperApi().getCookieJar().then((cookieManager) {
      cookieManager.cookieJar.loadForRequest(Uri.parse(url)).then((cookies) {
        for (var cookie in cookies) {
          WebViewCookieManager().setCookie(
            WebViewCookie(
              name: cookie.name,
              value: cookie.value,
              domain: cookie.domain ?? "",
            ),
          );
          WebViewCookieManager().setCookie(
            WebViewCookie(
              name: cookie.name,
              value: cookie.value,
              domain: url,
            ),
          );
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
