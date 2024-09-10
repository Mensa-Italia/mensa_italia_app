import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:stacked_services/stacked_services.dart';

class AddonDealsAddViewModel extends MasterModel {
  final DealModel? deal;
  // Form key for validation
  final formKey = GlobalKey<FormState>();

  // Text Editing Controllers for each form field
  final dealNameController = TextEditingController();
  final commercialSectorController = TextEditingController();
  TextEditingController locationController = TextEditingController();
  TextEditingController dateTimeEvent = TextEditingController();
  final detailsController = TextEditingController();
  final whoController = TextEditingController();
  final howToGetController = TextEditingController();
  final linkController = TextEditingController();
  final vatNumberController = TextEditingController();

  final contactName = TextEditingController();
  final contactEmail = TextEditingController();
  final contactPhone = TextEditingController();
  final contactNotes = TextEditingController();

  String? detailID;
  DateTimeRange? dateTimeOptions;
  LocationSelected? location;
  String selectedEligibility = "active_members";

  AddonDealsAddViewModel({this.deal}) {
    whoController.text = "Active Members";
    if (deal != null) {
      dealNameController.text = deal!.name;
      commercialSectorController.text = deal!.commercialSector;
      locationController.text = deal!.position?.name ?? "";
      if (deal?.starting != null && deal?.ending != null) {
        dateTimeOptions = DateTimeRange(
          start: deal!.starting!,
          end: deal!.ending!,
        );
        dateTimeEvent.text =
            "${DateFormat("dd/MM/yyyy HH:mm").format(deal!.starting!)} - ${DateFormat("dd/MM/yyyy HH:mm").format(deal!.ending!)}";
      }
      detailsController.text = deal!.details ?? "";
      whoController.text = deal!.who ?? "";
      howToGetController.text = deal!.howToGet ?? "";
      linkController.text = deal!.link ?? "";
      vatNumberController.text = deal!.vatNumber ?? "";
      Api().getDealsContacts(deal!.id).then((value) {
        if (value.isEmpty) return;
        contactName.text = value.first.name;
        contactEmail.text = value.first.email;
        contactPhone.text = value.first.phoneNumber ?? "";
        contactNotes.text = value.first.note ?? "";
        detailID = value.first.id;
        rebuildUi();
      });
    }
  }

  // Submit deal logic
  void submitDeal() async {
    if (formKey.currentState!.validate()) {
      setBusy(true);
      try {
        if (deal == null) {
          await Api().addDeal(
            name: dealNameController.text,
            commercialSector: commercialSectorController.text,
            details: detailsController.text,
            who: selectedEligibility,
            howToGet: howToGetController.text,
            link: linkController.text,
            vatNumber: vatNumberController.text,
            location: location,
            detailName: contactName.text,
            detailEmail: contactEmail.text,
            detailPhone: contactPhone.text,
            detailNote: contactNotes.text,
            starting: dateTimeOptions!.start,
            ending: dateTimeOptions!.end,
          );
          navigationService.back();
        } else {
          await Api().updateDeal(
            id: deal!.id,
            name: dealNameController.text,
            commercialSector: commercialSectorController.text,
            details: detailsController.text,
            who: selectedEligibility,
            howToGet: howToGetController.text,
            link: linkController.text,
            vatNumber: vatNumberController.text,
            location: location,
            detailId: detailID,
            detailName: contactName.text,
            detailEmail: contactEmail.text,
            detailPhone: contactPhone.text,
            detailNote: contactNotes.text,
            starting: dateTimeOptions!.start,
            ending: dateTimeOptions!.end,
          );
          navigationService.back();
        }
      } catch (_) {
        setBusy(false);
      }
    }
  }

  @override
  void dispose() {
    dealNameController.dispose();
    commercialSectorController.dispose();
    locationController.dispose();
    dateTimeEvent.dispose();
    detailsController.dispose();
    whoController.dispose();
    howToGetController.dispose();
    linkController.dispose();
    vatNumberController.dispose();
    super.dispose();
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
        dateTimeOptions = value;
        dateTimeEvent.text =
            "${DateFormat("dd/MM/yyyy HH:mm").format(value.start)} - ${DateFormat("dd/MM/yyyy HH:mm").format(value.end)}";
      }
    });
  }

  void selectEligibility() async {
    final ListOfEligibility = [
      "active_members",
      "active_members and relatives",
    ];
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
                    initialItem: ListOfEligibility.indexOf(selectedEligibility),
                  ),
                  onSelectedItemChanged: (int index) {
                    selectedEligibility = ListOfEligibility[index];
                    String eligibiltyString = "";
                    switch (selectedEligibility) {
                      case "active_members":
                        eligibiltyString = "Active Members";
                        break;
                      case "active_members and relatives":
                        eligibiltyString = "Active Members and Relatives";
                        break;
                    }
                    whoController.text = eligibiltyString;
                  },
                  children: List<Widget>.generate(
                    ListOfEligibility.length,
                    (int index) {
                      return Center(
                        child: Text(
                          ListOfEligibility[index],
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
  }
}
