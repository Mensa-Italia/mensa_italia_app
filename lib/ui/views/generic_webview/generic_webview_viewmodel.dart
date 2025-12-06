import 'dart:ui';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:webview_cookie_manager_plus/webview_cookie_manager_plus.dart';
import 'package:webview_flutter/webview_flutter.dart';

String jsForceLogin = """
    var form = document.querySelector('form');
    if (form) {
        // Get the email and password input fields
        var emailField = form.querySelector('input[name="email"]');
        var passwordField = form.querySelector('input[name="password"]');

        // Check if both fields exist
        if (emailField && passwordField) {
            // Set the values for email and password fields
            emailField.value = `{email}`;
            passwordField.value = `{password}`;
            form.submit();
        }
    }
""";

String jsBeautifyPage = """
const headerTag = document.querySelector('header');
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
""";

String jsRedirectToURL = """
window.location.href = `{url}`;
""";

class GenericWebviewViewModel extends MasterModel {
  @override
  String componentName = "views.generic_webview.title";
  WebviewCookieManager cookieManager = WebviewCookieManager();
  WebViewController? controller;
  double wholeOpacity = 0;
  final String url;
  bool _pageOpened = true;

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
                  if (url.startsWith(
                      "https://www.cloud32.it/Associazioni/utenti/login")) {
                    _pageOpened = false;
                    await controller!.runJavaScript(jsForceLogin
                        .replaceAll(
                          "{email}",
                          await ScraperApi().getStoredEmail(),
                        )
                        .replaceAll(
                          "{password}",
                          await ScraperApi().getStoredPassword(),
                        ));
                  } else {
                    await controller!.runJavaScript(jsBeautifyPage);
                    if (!_pageOpened && url != this.url) {
                      await controller!.runJavaScript(
                          jsRedirectToURL.replaceAll("{url}", this.url));
                    } else {
                      _pageOpened = true;
                    }
                  }
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
