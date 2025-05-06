import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/date_time_zone.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:omni_datetime_picker/omni_datetime_picker.dart';

class AddScheduleViewModel extends MasterModel {
  final formKey = GlobalKey<FormState>();

  TextEditingController locationController = TextEditingController();
  TextEditingController dateTimeStartEvent = TextEditingController();
  TextEditingController dateTimeEndEvent = TextEditingController();

  final EventScheduleModel? event;

  AddScheduleViewModel({this.event}) {
    if (event != null) {
      nameController.text = event!.title;
      descriptionController.text = event!.description;
      linkController.text = event!.infoLink;

      dateTimeOptions = RangeDateTimeZone.fromDateTime(
        start: event!.whenStart,
        end: event!.whenEnd,
      );
      dateTimeStartEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getStart());
      dateTimeEndEvent.text = DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions.getEnd());
      rebuildUi();
    }
  }

  //FORM DATA
  XFile? image;
  TextEditingController nameController = TextEditingController();
  TextEditingController descriptionController = TextEditingController();
  TextEditingController linkController = TextEditingController();
  RangeDateTimeZone dateTimeOptions = RangeDateTimeZone();
  LocationSelected? location;
  bool isNational = false;
  bool isOnline = false;
  // END FORM DATA

  void pickLocation() {
    navigationService.navigateToMapPickerView().then((value) {
      if (value != null && value is LocationSelected) {
        location = value;
        locationController.text = value.locationName;
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

  void save() {
    if (formKey.currentState!.validate()) {
      setBusy(true);
      EventScheduleModel eventSchedule = EventScheduleModel(
        id: event?.id,
        title: nameController.text,
        description: descriptionController.text,
        whenStart: dateTimeOptions!.start!,
        whenEnd: dateTimeOptions!.end!,
        maxExternalGuests: 0,
        price: 0,
        infoLink: linkController.text,
        isSubscriptable: false,
      );
      navigationService.back(result: eventSchedule);
    }
  }

  void deleteEvent() {
    setBusy(true);
    EventScheduleModel eventSchedule = EventScheduleModel(
      id: "DELETE:${event?.id ?? ""}",
      title: nameController.text,
      description: descriptionController.text,
      whenStart: dateTimeOptions!.start!,
      whenEnd: dateTimeOptions!.end!,
      maxExternalGuests: 0,
      price: 0,
      infoLink: linkController.text,
      isSubscriptable: false,
    );
    navigationService.back(result: eventSchedule);
  }

  @override
  void dispose() {
    nameController.dispose();
    descriptionController.dispose();
    linkController.dispose();
    dateTimeStartEvent.dispose();
    dateTimeEndEvent.dispose();
    locationController.dispose();
    super.dispose();
  }
}
