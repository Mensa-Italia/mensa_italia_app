import 'dart:ui';

import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:webview_cookie_manager/webview_cookie_manager.dart';
import 'package:webview_flutter/webview_flutter.dart';

class GenericWebviewViewModel extends MasterModel {
  WebviewCookieManager cookieManager = WebviewCookieManager();
  WebViewController? controller;
  double wholeOpacity = 0;
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
              onPageStarted: (String url) {
                wholeOpacity = 0;
                rebuildUi();
              },
              onPageFinished: (String url) async {
                try {
                  await controller!.runJavaScript("""const headerTag = document.querySelector('header');
if (headerTag) {
  headerTag.remove();
}
const brTags = document.querySelectorAll('br');
document.body.style.backgroundColor = 'transparent';
// Loop through the first 5 <br> tags and remove them
for (let i = 0; i < 5; i++) {
  if (brTags[i]) {
    brTags[i].remove();
  } else {
    break; // Exit the loop if there are fewer than 5 <br> tags
  }
}

const footerDiv = document.querySelector('div.footer');

// Check if the <div> with the class "footer" exists, and if so, remove it
if (footerDiv) {
  footerDiv.remove();
}
""");
                } catch (_) {}
                wholeOpacity = 1;
                rebuildUi();
              },
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
