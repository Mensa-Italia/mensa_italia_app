import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
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
    return user.powers.contains(power) || user.powers.contains("super");
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

  allowControlDeals() {
    return hasPower("deals");
  }

  isSuper() {
    return hasPower("super");
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

  Future<Position> determinePosition() async {
    bool serviceEnabled;
    LocationPermission permission;

    serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return Future.error('Location services are disabled.');
    }

    permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return Future.error('Location permissions are denied');
      }
    }

    if (permission == LocationPermission.deniedForever) {
      return Future.error(
          'Location permissions are permanently denied, we cannot request permissions.');
    }

    return await Geolocator.getCurrentPosition();
  }
}
