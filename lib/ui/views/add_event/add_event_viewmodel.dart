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
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:sentry_flutter/sentry_flutter.dart';

class AddEventViewModel extends MasterModel {
  final formKey = GlobalKey<FormState>();
  Uint8List? imageBytes;
  TextEditingController locationController = TextEditingController();
  TextEditingController dateTimeEvent = TextEditingController();
  List<EventScheduleModel> eventSchedules = [];

  //FORM DATA
  XFile? image;
  TextEditingController nameController = TextEditingController();
  TextEditingController descriptionController = TextEditingController();
  TextEditingController linkController = TextEditingController();
  RangeDateTimeZone? dateTimeOptions;
  LocationSelected? location;
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
      dateTimeEvent.text = "${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.end)}";
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
      if ((dateTimeOptions?.start ?? DateTime.now()).isBefore(DateTime.now().add(Duration(days: 1)))) {
        dialogService
            .showDialog(
          title: 'views.add_event_viewmodel.date_error.title'.tr(),
          description: 'views.add_event_viewmodel.date_error.description'.tr(),
        )
            .then((value) {
          pickDateTime();
        });
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
            startDate: dateTimeOptions!.start,
            endDate: dateTimeOptions!.end,
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
            startDate: dateTimeOptions!.start,
            endDate: dateTimeOptions!.end,
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
    navigationService.navigateToMapPickerView().then((value) {
      if (value != null && value is LocationSelected) {
        location = value;
        locationController.text = value.locationName;
      }
    });
  }

  void pickDateTime() {
    pickStartEndTime(
      start: dateTimeOptions?.start,
      end: dateTimeOptions?.end,
    ).then((value) {
      if (value != null) {
        if (!allowControlEvents() && value.end.difference(value.start).inDays > 2) {
          dialogService
              .showDialog(
            title: 'views.add_event_viewmodel.spot_event_error.title'.tr(),
            description: 'views.add_event_viewmodel.spot_event_error.description'.tr(),
          )
              .then((value) {
            pickDateTime();
          });
        } else {
          dateTimeOptions = value;
          dateTimeEvent.text = "${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.end)}";
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
    dateTimeEvent.dispose();
    nameController.dispose();
    descriptionController.dispose();
    linkController.dispose();
    super.dispose();
  }
}
