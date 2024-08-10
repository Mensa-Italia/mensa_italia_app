import 'dart:typed_data';

import 'package:board_datetime_picker/board_datetime_picker.dart';
import 'package:board_datetime_picker/src/board_datetime_widget.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:stacked_services/stacked_services.dart';

class AddEventViewModel extends MasterModel {
  final formKey = GlobalKey<FormState>();
  Uint8List? imageBytes;
  TextEditingController locationController = TextEditingController();
  TextEditingController dateTimeEvent = TextEditingController();

  //FORM DATA
  XFile? image;
  TextEditingController nameController = TextEditingController();
  TextEditingController descriptionController = TextEditingController();
  TextEditingController linkController = TextEditingController();
  BoardDateTimeMultiSelection? dateTimeOptions;
  LocationSelected? location;
  bool isNational = false;
  bool isOnline = false;
  // END FORM DATA

  void pickImage() async {
    final ImagePicker picker = ImagePicker();

    final XFile? imaget = await picker.pickImage(source: ImageSource.gallery);
    if (imaget != null) {
      image = imaget;
    } else {
      final LostDataResponse response = await picker.retrieveLostData();
      if (response.isEmpty) {
        return;
      }
      final List<XFile>? files = response.files;
      if (files != null) {
        image = files.first;
      }
    }

    if (image != null) {
      image!.readAsBytes().then((value) {
        imageBytes = value;
        rebuildUi();
      });
    }
  }

  void addEvent() async {
    if (formKey.currentState!.validate() && !isBusy) {
      setBusy(true);
      try {
        await Api().createEvent(
          name: nameController.text,
          description: descriptionController.text,
          image: image,
          location: location,
          link: linkController.text,
          startDate: dateTimeOptions!.start,
          endDate: dateTimeOptions!.end,
          isNational: isNational,
          isOnline: isOnline,
        );
        navigationService.back();
      } catch (_) {}
      setBusy(false);
    }
  }

  void pickLocation() {
    navigationService.navigateToMapPickerView().then((value) {
      if (value != null && value is LocationSelected) {
        location = value;
        locationController.text = value.locationName;
      }
    });
  }

  void pickDateTime() {
    showBoardDateTimeMultiPicker(
      context: StackedService.navigatorKey!.currentContext!,
      startDate: DateTime.now(),
      endDate: DateTime.now().add(const Duration(days: 365 * 10)),
      pickerType: DateTimePickerType.datetime,
      options: BoardDateTimeOptions(
        startDayOfWeek: DateTime.monday,
        activeColor: kcPrimaryColor,
        foregroundColor: Colors.white,
        pickerFormat: "dMy",
      ),
      useSafeArea: true,
    ).then((value) {
      if (value != null) {
        dateTimeOptions = value;
        dateTimeEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(value.start) + " - " + DateFormat("dd/MM/yyyy HH:mm").format(value.end);
      }
    });
  }

  void toggleNational(bool value) {
    isNational = value;
    rebuildUi();
  }

  void toggleOnline(bool value) {
    isOnline = value;
    rebuildUi();
  }
}
