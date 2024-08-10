import 'dart:typed_data';

import 'package:board_datetime_picker/board_datetime_picker.dart';
import 'package:board_datetime_picker/src/board_datetime_widget.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
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

  EventModel? eventEditing;
  bool get isEditing => eventEditing != null;

  AddEventViewModel({EventModel? event}) {
    if (event != null) {
      eventEditing = event;
      nameController.text = event.name;
      descriptionController.text = event.description;
      linkController.text = event.infoLink;

      dateTimeOptions = BoardDateTimeMultiSelection(
        start: event.whenStart,
        end: event.whenEnd,
      );
      dateTimeEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.start) + " - " + DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.end);
      location = LocationSelected(
        locationName: event.position!.name,
        coordinates: event.position!.toLatLng(),
      );
      locationController.text = location!.locationName;
      isNational = event.isNational;
      isOnline = event.position == null;
    }
  }

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
        if (isEditing) {
          print("editing");
          await Api().updateEvent(
            id: eventEditing!.id,
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
        } else {
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
        }
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
      startDate: dateTimeOptions?.start ?? DateTime.now().add(Duration(days: 2)),
      endDate: dateTimeOptions?.end ?? DateTime.now().add(Duration(hours: 2, days: 2)),
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

  void deleteEvent() async {
    try {
      if (eventEditing != null) {
        setBusy(true);
        await dialogService
            .showConfirmationDialog(
          title: 'Delete event',
          description: 'Are you sure you want to delete this event?',
          confirmationTitle: 'Yes',
          cancelTitle: 'No',
        )
            .then(
          (value) async {
            if (value != null && value.confirmed == true) {
              await Api().deleteEvent(eventEditing!.id);
              navigationService.back();
            }
          },
        );
      }
    } catch (_) {}
    setBusy(false);
  }
}
