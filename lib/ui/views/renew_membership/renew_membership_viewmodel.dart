import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class RenewMembershipViewModel extends MasterModel {
  RenewMembershipViewModel() {}

  void goToRenewMembershipWebview() {
    navigationService.navigateToGenericWebviewView(
      url: "https://www.cloud32.it/Associazioni/utenti/richirinnovo",
      title: "Renew Membership",
      previousPageTitle: "Back",
    );
  }

  void logout() {
    ScraperApi().logout().then((value) {
      navigationService.replaceWith(Routes.loginView);
    });
  }
}
