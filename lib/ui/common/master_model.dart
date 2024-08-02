import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.locator.dart';
import 'package:mensa_italia_app/model/user.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class MasterModel extends ReactiveViewModel {
  final _dialogService = locator<DialogService>();
  final _navigationService = locator<NavigationService>();

  DialogService get dialogService => _dialogService;
  NavigationService get navigationService => _navigationService;

  UserModel get user {
    return Api().getUser()!;
  }

  hasPower(String power) {
    return user.powers.contains(power);
  }

  allowTestMakerAddon() {
    return hasPower("testmakers");
  }

  allowControlSigs() {
    return hasPower("sigs");
  }

  allowControlEvents() {
    return hasPower("events");
  }

  allowControlAddons() {
    return hasPower("addons");
  }

  Future showBeautifulBottomSheet({required Widget child}) async {
    return await showCupertinoModalBottomSheet(
      context: StackedService.navigatorKey!.currentContext!,
      backgroundColor: Colors.transparent,
      elevation: 0,
      builder: (context) => Material(
        color: Colors.transparent,
        child: SingleChildScrollView(
          controller: ModalScrollController.of(context),
          padding:
              EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
          child: child,
        ),
      ),
    );
  }
}
