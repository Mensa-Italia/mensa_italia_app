
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';

class AddScheduleViewModel extends MasterModel {
  final formKey = GlobalKey<FormState>();

  TextEditingController locationController = TextEditingController();
  TextEditingController dateTimeEvent = TextEditingController();

  final EventScheduleModel? event;

  AddScheduleViewModel({this.event}) {
    if (event != null) {
      nameController.text = event!.title;
      descriptionController.text = event!.description;
      linkController.text = event!.infoLink;

      dateTimeOptions = DateTimeRange(
        start: event!.whenStart,
        end: event!.whenEnd,
      );
      dateTimeEvent.text = "${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.end)}";

      rebuildUi();
    }
  }

  //FORM DATA
  XFile? image;
  TextEditingController nameController = TextEditingController();
  TextEditingController descriptionController = TextEditingController();
  TextEditingController linkController = TextEditingController();
  DateTimeRange? dateTimeOptions;
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

  void pickDateTime() {
    pickStartEndTime(
      start: dateTimeOptions?.start,
      end: dateTimeOptions?.end,
    ).then((value) {
      if (value != null) {
        dateTimeOptions = value;
        dateTimeEvent.text = "${DateFormat("dd/MM/yyyy HH:mm").format(value.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(value.end)}";
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
        whenStart: dateTimeOptions!.start,
        whenEnd: dateTimeOptions!.end,
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
      whenStart: dateTimeOptions!.start,
      whenEnd: dateTimeOptions!.end,
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
    dateTimeEvent.dispose();
    locationController.dispose();
    super.dispose();
  }
}
