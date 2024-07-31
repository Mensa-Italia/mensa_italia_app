import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/app/app.router.dart';

class StartupViewModel extends MasterModel {
  Future runStartupLogic() async {
    await ScraperApi().init();
    ScraperApi().isPasswordEmailStored().then((existsStored) async {
      if (existsStored) {
        final email = await ScraperApi().getStoredEmail();
        final password = await ScraperApi().getStoredPassword();
        Api().login(email: email, password: password).then((isLogged) {
          if (isLogged) {
            if (!user.isMembershipActive) {
              navigationService.replaceWith(Routes.homeView);
            } else {
              navigationService.replaceWith(Routes.renewMembershipView);
            }
          } else {
            navigationService.replaceWith(Routes.loginView);
          }
        });
      } else {
        await Future.delayed(const Duration(seconds: 5));
        navigationService.replaceWith(Routes.loginView);
      }
    });
  }
}
