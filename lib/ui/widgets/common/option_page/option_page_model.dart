import 'dart:io';

import 'package:easy_localization/easy_localization.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:url_launcher/url_launcher.dart';

class OptionPageModel extends MasterModel {
  @override
  String componentName = "views.settings.title";
  String version = "20.00.00";

  OptionPageModel() {
    PackageInfo.fromPlatform().then((value) {
      version = value.version;
      rebuildUi();
    });
  }

  void logout() {
    Api().removeThisDevice().then((_) {
      Api().logout();
      ScraperApi().logout().then((value) {
        navigationService.replaceWith(Routes.loginView);
      });
    });
  }

  changePassword() {
    navigationService.navigateToGenericWebviewView(
      url: "https://www.cloud32.it/Associazioni/utenti/password/stdreset",
      title: "Change Password",
      previousPageTitle: componentName.tr(),
    );
  }

  void openPrivacyPolicy() {
    navigationService.navigateToDocumentViewerView(
      downlaodUrl: "https://www.mensa.it/wp-content/uploads/2018/04/Informativa-Privacy-Mensa-Italia_Ver._Mar-2018.pdf",
      title: "Privacy Policy",
      previousPageTitle: componentName.tr(),
    );
  }

  editProfile() {
    ScraperApi().getMyProfileSetting(name: "modifica profilo").then((value) {
      navigationService.navigateToGenericWebviewView(
        url: value,
        title: "Edit Profile",
        previousPageTitle: componentName.tr(),
      );
    });
  }

  renewSubscription() {
    navigationService.navigateToGenericWebviewView(
      url: "https://www.cloud32.it/Associazioni/utenti/rinnovo",
      title: "Renew Membership",
      previousPageTitle: componentName.tr(),
    );
  }

  openCalendarLinker() {
    navigationService.navigateToCalendarLinkerView(
      previousPageTitle: componentName,
    );
  }

  openPaymentMethodManager() {
    navigationService.navigateToPaymentMethodManagerView(
      previousPageTitle: componentName,
    );
  }

  openNotificationSettings() {
    navigationService.navigateToNotificationViewView(
      previousPageTitle: componentName,
    );
  }

  openDonationPage() {
    if (Platform.isIOS) {
      Api().settings().then((settings) async {
        if (settings["donation_link_on_ios"] == "false") {
          navigationService.navigateToMakeDonationView(
            previousPageTitle: componentName,
          );
          return;
        }
        Uri url = Uri.parse(settings["donation_stripe_link"] ?? "https://www.mensa.it/sostienici/");
        url = url.replace(queryParameters: {
          "locked_prefilled_email": user.email,
        });
        launchUrl(url);
      });
      return;
    }
    navigationService.navigateToMakeDonationView(
      previousPageTitle: componentName,
    );
  }

  openTicketPage() {
    navigationService.navigateToTicketsView(
      previousPageTitle: componentName,
    );
  }

  openReceipts() {
    navigationService.navigateToReceiptsView(
      previousPageTitle: componentName,
    );
  }

  openDevices() {
    navigationService.navigateToDevicesView(
      previousPageTitle: componentName,
    );
  }
}
