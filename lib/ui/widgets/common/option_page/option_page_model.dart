import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:package_info_plus/package_info_plus.dart';

class OptionPageModel extends MasterModel {
  String version = "20.00.00";

  OptionPageModel() {
    PackageInfo.fromPlatform().then((value) {
      version = value.version;
      rebuildUi();
    });
  }

  void logout() {
    ScraperApi().logout().then((value) {
      navigationService.replaceWith(Routes.loginView);
    });
  }

  changePassword() {
    navigationService.navigateToGenericWebviewView(
      url: "https://www.cloud32.it/Associazioni/utenti/password/stdreset",
      title: "Change Password",
      previousPageTitle: "Settings",
    );
  }

  void openPrivacyPolicy() {
    navigationService.navigateToDocumentViewerView(
      downlaodUrl:
          "https://www.mensa.it/wp-content/uploads/2018/04/Informativa-Privacy-Mensa-Italia_Ver._Mar-2018.pdf",
      title: "Privacy Policy",
      previousPageTitle: "Settings",
    );
  }

  editProfile() {
    ScraperApi().getMyProfileSetting(name: "modifica profilo").then((value) {
      navigationService.navigateToGenericWebviewView(
        url: value,
        title: "Edit Profile",
        previousPageTitle: "Settings",
      );
    });
  }

  renewSubscription() {
    navigationService.navigateToGenericWebviewView(
      url: "https://www.cloud32.it/Associazioni/utenti/rinnovo",
      title: "Renew Membership",
      previousPageTitle: "Settings",
    );
  }

  openCalendarLinker() {
    navigationService.navigateToCalendarLinkerView();
  }
}
