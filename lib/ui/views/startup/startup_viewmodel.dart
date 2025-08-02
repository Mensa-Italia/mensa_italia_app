import 'package:app_version_update/app_version_update.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/services/notify_sse.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/app/app.router.dart';

class StartupViewModel extends MasterModel {
  Future runVersionCheck() async {
    await AppVersionUpdate.checkForUpdates(
      appleId: "1524200080",
      playStoreId: "it.mensa.app",
    ).then((data) async {
      if (data.canUpdate ?? false) {
        await AppVersionUpdate.showAlertUpdate(
          context: context,
          appVersionResult: data,
          title: "dialog.update.title".tr(),
          content: "dialog.update.content".tr(),
          mandatory: true,
        );
      }
    });
  }

  Future runStartupLogic() async {
    Api().settings().then((value) async {
      Stripe.publishableKey = value["stripe_key"] ?? "";
      Stripe.urlScheme = "mensa";
      Stripe.merchantIdentifier = "merchant.it.mensa.app";
      Stripe.instance.applySettings();

      ScraperApi().init();
      ScraperApi().isPasswordEmailStored().then((existsStored) async {
        if (existsStored) {
          final email = await ScraperApi().getStoredEmail();
          final password = await ScraperApi().getStoredPassword();
          Api().login(email: email, password: password).then((isLogged) {
            if (isLogged) {
              NotifySSE().start();
              if (user.isMembershipActive) {
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
    });
  }
}
