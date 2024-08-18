import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

class InputTextDialogModel extends BaseViewModel {
  final DialogRequest request;
  TextEditingController textController = TextEditingController();

  InputTextDialogModel({required this.request}) {
    initialise();
  }

  void initialise() {
    textController.text = request.data ?? '';
  }
}
