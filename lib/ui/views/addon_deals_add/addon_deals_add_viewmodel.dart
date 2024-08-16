import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonDealsAddViewModel extends MasterModel {
  // Form key for validation
  final formKey = GlobalKey<FormState>();

  // Text Editing Controllers for each form field
  final dealNameController = TextEditingController();
  final commercialSectorController = TextEditingController();
  final positionController = TextEditingController();
  final startingDateController = TextEditingController();
  final endingDateController = TextEditingController();
  final detailsController = TextEditingController();
  final whoController = TextEditingController();
  final howToGetController = TextEditingController();
  final linkController = TextEditingController();
  final vatNumberController = TextEditingController();

  // Submit deal logic
  void submitDeal() {
    if (formKey.currentState!.validate()) {
    }
  }

  @override
  void dispose() {
    dealNameController.dispose();
    commercialSectorController.dispose();
    positionController.dispose();
    startingDateController.dispose();
    endingDateController.dispose();
    detailsController.dispose();
    whoController.dispose();
    howToGetController.dispose();
    linkController.dispose();
    vatNumberController.dispose();
    super.dispose();
  }
}
