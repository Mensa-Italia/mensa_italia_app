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
      return "Please enter a valid email";
    }
  }

  String? validatePassword(String? value) {
    if (value != null && value.isNotEmpty) {
      return null;
    } else {
      return "Password cannot be empty";
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
          navigationService.replaceWith(Routes.homeView);
        } else {
          dialogService.showCustomDialog(
            variant: DialogType.infoAlert,
            title: "Login Failed",
            description: "Invalid email or password",
          );
        }
      }).catchError((e) {
        setBusy(false);
        dialogService.showCustomDialog(
          variant: DialogType.infoAlert,
          title: "Login Failed",
          description: "An error occurred while logging in",
        );
      });
    }
  }
}
