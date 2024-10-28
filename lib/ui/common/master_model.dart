import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.locator.dart';
import 'package:mensa_italia_app/model/user.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:omni_datetime_picker/omni_datetime_picker.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class MasterModel extends ReactiveViewModel {
  final _dialogService = locator<DialogService>();
  final _navigationService = locator<NavigationService>();

  DialogService get dialogService => _dialogService;
  NavigationService get navigationService => _navigationService;

  BuildContext get context => StackedService.navigatorKey!.currentContext!;

  UserModel get user {
    return Api().getUser()!;
  }

  hasPower(String power) {
    return user.powers.contains(power) || user.powers.contains("${power}_helper") || user.powers.contains("super");
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
      topRadius: const Radius.circular(30),
      elevation: 0,
      builder: (context) => Material(
        color: Colors.transparent,
        child: SingleChildScrollView(
          controller: ModalScrollController.of(context),
          padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
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
      return Future.error('Location permissions are permanently denied, we cannot request permissions.');
    }

    return await Geolocator.getCurrentPosition();
  }

  Future<DateTimeRange?> pickStartEndTime({DateTime? start, DateTime? end}) async {
    List<DateTime>? dateTimeList = await showOmniDateTimeRangePicker(
      context: context,
      startInitialDate: start,
      startFirstDate: DateTime(1600).subtract(const Duration(days: 3652)),
      startLastDate: DateTime.now().add(
        const Duration(days: 3652),
      ),
      endInitialDate: end,
      endFirstDate: DateTime(1600).subtract(const Duration(days: 3652)),
      endLastDate: DateTime.now().add(
        const Duration(days: 3652),
      ),
      is24HourMode: true,
      isShowSeconds: false,
      minutesInterval: 5,
      borderRadius: const BorderRadius.all(Radius.circular(16)),
      constraints: const BoxConstraints(
        maxWidth: 350,
        maxHeight: 650,
      ),
      transitionBuilder: (context, anim1, anim2, child) {
        return FadeTransition(
          opacity: anim1.drive(
            Tween(
              begin: 0,
              end: 1,
            ),
          ),
          child: child,
        );
      },
      transitionDuration: const Duration(milliseconds: 200),
      barrierDismissible: true,
      startSelectableDayPredicate: (dateTime) {
        return true;
      },
      endSelectableDayPredicate: (dateTime) {
        if (start != null) {
          return dateTime.isAfter(start);
        } else {
          return true;
        }
      },
    );

    if (dateTimeList != null) {
      return DateTimeRange(start: dateTimeList[0], end: dateTimeList[1]);
    } else {
      return null;
    }
  }

  Future<String?> cupertinoModalPicker({required int initialItem, required List<String> items}) async {
    String data = items[initialItem];
    await showCupertinoModalPopup<void>(
      context: StackedService.navigatorKey!.currentContext!,
      builder: (BuildContext context) => Container(
        height: 216,
        padding: const EdgeInsets.only(top: 6.0),
        margin: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        color: CupertinoColors.systemBackground.resolveFrom(context),
        child: SafeArea(
          top: false,
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Spacer(),
                  CupertinoButton(
                    child: const Text(
                      'Done',
                      style: TextStyle(
                        color: kcPrimaryColor,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    onPressed: () {
                      Navigator.of(context).pop();
                    },
                  ),
                ],
              ),
              Expanded(
                child: CupertinoPicker(
                  itemExtent: 32.0,
                  scrollController: FixedExtentScrollController(
                    initialItem: initialItem,
                  ),
                  onSelectedItemChanged: (int index) {
                    data = items[index];
                  },
                  children: List<Widget>.generate(
                    items.length,
                    (int index) {
                      return Center(
                        child: Text(
                          items[index],
                        ),
                      );
                    },
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    return data;
  }
}
