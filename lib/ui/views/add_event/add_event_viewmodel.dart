import 'dart:typed_data';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/date_time_zone.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/event_card_generator/event_card_generator.dart';
import 'package:omni_datetime_picker/omni_datetime_picker.dart';
import 'package:sentry_flutter/sentry_flutter.dart';

class AddEventViewModel extends MasterModel {
  final formKey = GlobalKey<FormState>();
  Uint8List? imageBytes;
  TextEditingController locationController = TextEditingController();
  TextEditingController dateTimeStartEvent = TextEditingController();
  TextEditingController dateTimeEndEvent = TextEditingController();
  List<EventScheduleModel> eventSchedules = [];

  //FORM DATA
  XFile? image;
  TextEditingController nameController = TextEditingController();
  TextEditingController descriptionController = TextEditingController();
  TextEditingController linkController = TextEditingController();
  RangeDateTimeZone dateTimeOptions = RangeDateTimeZone();
  LocationModel? location;
  bool isNational = false;
  bool isOnline = false;
  bool isSpot = false;
  // END FORM DATA

  EventModel? eventEditing;
  bool get isEditing => eventEditing != null;

  AddEventViewModel({EventModel? event}) {
    if (event != null) {
      Api().getEventSchedules(event.id).then((value) {
        eventSchedules.clear();
        eventSchedules.addAll(value);
        rebuildUi();
      });
      eventEditing = event;
      nameController.text = event.name;
      descriptionController.text = event.description;
      linkController.text = event.infoLink;

      dateTimeOptions = RangeDateTimeZone.fromDateTime(
        start: event.whenStart,
        end: event.whenEnd,
      );
      dateTimeStartEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getStart());
      dateTimeEndEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getEnd());
      if (event.position != null) {
        location = event.position;
        locationController.text = location!.getAddress();
      }
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

  void generateImage() {
    MasterModel.showBeautifulBottomSheetInstance(
      child: EventCardGenerator(),
    ).then((value) {
      if (value != null && value is Uint8List) {
        image = XFile.fromData(value, name: "event_card.png", mimeType: "image/png");
        image?.readAsBytes().then((bytes) {
          imageBytes = bytes;
          rebuildUi();
        });
      }
    });
  }

  void addEvent() async {
    if (formKey.currentState!.validate() && !isBusy) {
      setBusy(true);
      if (!dateTimeOptions.isValidRange()) {
        dialogService.showDialog(
          title: 'views.add_event_viewmodel.date_error.title'.tr(),
          description: 'views.add_event_viewmodel.date_error.description'.tr(),
        );
        setBusy(false);
        return;
      }
      try {
        if (!allowControlEvents()) {
          isOnline = false;
          isNational = false;
          isSpot = true;
        }
        if (isEditing) {
          await Api().updateEvent(
            id: eventEditing!.id,
            name: nameController.text,
            description: descriptionController.text,
            image: image,
            location: location,
            link: linkController.text,
            startDate: dateTimeOptions.start!,
            endDate: dateTimeOptions.end!,
            isNational: isNational,
            isOnline: isOnline,
            schedules: eventSchedules,
            isSpot: isSpot,
          );
        } else {
          await Api().createEvent(
            name: nameController.text,
            description: descriptionController.text,
            image: image,
            location: location,
            link: linkController.text,
            startDate: dateTimeOptions.start!,
            endDate: dateTimeOptions.end!,
            isNational: isNational,
            isOnline: isOnline,
            schedules: eventSchedules,
            isSpot: isSpot,
          );
        }
        navigationService.back();
      } catch (exception, stackTrace) {
        Sentry.captureException(exception, stackTrace: stackTrace);
      }
      setBusy(false);
    }
  }

  void pickLocation() {
    navigationService.navigateToLocationListPickerView().then((value) {
      if (value != null && value is LocationModel) {
        location = value;
        locationController.text = value.getAddress();
        rebuildUi();
      }
    });
  }

  void pickStartTime() {
    showOmniDateTimePicker(
      context: context,
      is24HourMode: true,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(
        const Duration(days: 365 * 10),
      ),
    ).then((value) {
      if (value != null) {
        dateTimeOptions.setStart(value);
        dateTimeStartEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getStart());
        if (dateTimeOptions.isValidRange()) {
          dateTimeEndEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getEnd());
        } else {
          dateTimeOptions.clearEnd();
          dateTimeEndEvent.text = "";
        }
      }
    });
  }

  void pickEndTime() {
    showOmniDateTimePicker(
      context: context,
      is24HourMode: true,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(
        const Duration(days: 365 * 10),
      ),
    ).then((value) {
      if (value != null) {
        dateTimeOptions.setEnd(value);
        if (dateTimeOptions.isValidRange()) {
          dateTimeEndEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getEnd());
        } else {
          dateTimeOptions.clearEnd();
          dateTimeEndEvent.text = "";
        }
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

  void editSchedule() {
    navigationService.navigateToAddEventScheduleListView(
      eventSchedules: eventSchedules,
    );
  }

  @override
  void dispose() {
    locationController.dispose();
    dateTimeStartEvent.dispose();
    dateTimeEndEvent.dispose();
    nameController.dispose();
    descriptionController.dispose();
    linkController.dispose();
    super.dispose();
  }
}
