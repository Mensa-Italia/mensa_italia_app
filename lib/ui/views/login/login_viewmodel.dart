import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.dialogs.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:validation_pro/validate.dart';

class LoginViewModel extends MasterModel {
  final GlobalKey<FormState> formKey = GlobalKey<FormState>();
  String email = "";
  String password = "";

  String? validateEmail(String? value) {
    if (Validate.isEmail(value ?? "")) {
      return null;
    } else {
      return "views.signin.form.field.error.email".tr();
    }
  }

  String? validatePassword(String? value) {
    if (value != null && value.isNotEmpty) {
      return null;
    } else {
      return "views.signin.form.field.error.password".tr();
    }
  }

  String? saveEmail(String? value) {
    email = value ?? "";
    return null;
  }

  String? savePassword(String? value) {
    password = value ?? "";
    return null;
  }

  void doLogin() {
    if (isBusy) return;
    setBusy(true);
    if (formKey.currentState?.validate() ?? false) {
      formKey.currentState?.save();
      Api().login(email: email, password: password).then((res) {
        setBusy(false);
        if (res) {
          if (user.isMembershipActive) {
            navigationService.replaceWith(Routes.homeView);
          } else {
            navigationService.replaceWith(Routes.renewMembershipView);
          }
        } else {
          dialogService.showCustomDialog(
            variant: DialogType.infoAlert,
            title: "views.signin.result.error.invalidcredential.title".tr(),
            description:
                "views.signin.result.error.invalidcredential.body".tr(),
          );
        }
      }).catchError((e) {
        setBusy(false);
        dialogService.showCustomDialog(
          variant: DialogType.infoAlert,
          title: "views.signin.result.error.generic.title".tr(),
          description: "views.signin.result.error.generic.body".tr(),
        );
      });
    }
  }

  void goToResetPassword() {
    navigationService.navigateToGenericWebviewView(
      url:
          "https://www.cloud32.it/Associazioni/utenti/password/reset?codass=170734",
      title: "Reset Password",
      previousPageTitle: "views.signin.title2".tr(),
    );
  }
}
