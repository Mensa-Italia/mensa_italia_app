import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class OptionPageModel extends MasterModel {
  void logout() {
    ScraperApi().logout().then((value) {
      navigationService.replaceWith(Routes.loginView);
    });
  }

  changePassword() {
    navigationService.navigateToGenericWebviewView(url: "https://www.cloud32.it/Associazioni/utenti/password/stdreset");
  }

  void openPrivacyPolicy() {
    navigationService.navigateToGenericWebviewView(url: "https://www.mensa.it/wp-content/uploads/2018/04/Informativa-Privacy-Mensa-Italia_Ver._Mar-2018.pdf");
  }

  editProfile() {
    ScraperApi().getMyProfileSetting(name: "modifica profilo").then((value) {
      navigationService.navigateToGenericWebviewView(url: value);
    });
  }

  renewSubscription() {
    navigationService.navigateToGenericWebviewView(url: "https://www.cloud32.it/Associazioni/utenti/rinnovo");
  }
}
