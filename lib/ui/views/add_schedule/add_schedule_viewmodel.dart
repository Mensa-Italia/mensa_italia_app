import 'package:board_datetime_picker/board_datetime_picker.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:board_datetime_picker/src/board_datetime_widget.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event_schedule.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:stacked_services/stacked_services.dart';

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

      dateTimeOptions = BoardDateTimeMultiSelection(
        start: event!.whenStart,
        end: event!.whenEnd,
      );
      dateTimeEvent.text =
          "${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(dateTimeOptions!.end)}";

      rebuildUi();
    }
  }

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
      startDate:
          dateTimeOptions?.start ?? DateTime.now().add(Duration(days: 2)),
      endDate: dateTimeOptions?.end ??
          DateTime.now().add(Duration(hours: 2, days: 2)),
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
        dateTimeEvent.text =
            "${DateFormat("dd/MM/yyyy HH:mm").format(value.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(value.end)}";
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
