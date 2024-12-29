import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class BottomSheetAddSigModel extends MasterModel {
  SigModel? sig;
  XFile? image;

  //sig_facebook, sig, local, chat_whatsapp, chat_telegram, chat
  Map<String, String> sigTypes = {
    'sig_facebook': 'SIG Facebook',
    'sig': 'SIG Generic',
    'local': 'Local group',
    'chat_whatsapp': 'Chat WhatsApp',
    'chat_telegram': 'Chat Telegram',
    'chat': 'Chat',
  };

  String sigType = 'sig';

  TextEditingController nameController = TextEditingController();

  GlobalKey<FormState> formKey = GlobalKey<FormState>();

  TextEditingController linkController = TextEditingController();
  TextEditingController sigTypeController = TextEditingController(
    text: 'SIG Generic',
  );

  Uint8List? imageBytes;

  BottomSheetAddSigModel({this.sig}) {
    if (sig != null) {
      nameController.text = sig!.name;
      linkController.text = sig!.link;
      rebuildUi();
    }
  }

  void addSig() async {
    setBusy(true);
    try {
      if (formKey.currentState!.validate()) {
        if (sig != null) {
          var done = await Api().updateSig(
            id: sig!.id,
            name: nameController.text,
            link: linkController.text,
            image: image,
            sigType: sigType,
          );
          if (done) {
            navigationService.back();
          }
        } else {
          var done = await Api().addSig(
            name: nameController.text,
            link: linkController.text,
            image: image,
            sigType: sigType,
          );
          if (done) {
            navigationService.back();
          }
        }
      }
    } catch (_) {}
    setBusy(false);
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

  void deleteSig() {
    dialogService
        .showConfirmationDialog(
      title: 'Delete SIG',
      description: 'Are you sure you want to delete this SIG?',
      confirmationTitle: 'Yes',
      cancelTitle: 'No',
    )
        .then((response) {
      if (response?.confirmed == true) {
        setBusy(true);
        Api().deleteSig(sig!.id).then((value) {
          setBusy(false);
          navigationService.back();
        });
      }
    });
  }

  @override
  void dispose() {
    nameController.dispose();
    linkController.dispose();

    super.dispose();
  }

  onTapSigType() {
    cupertinoModalPicker(
            title: 'Type',
            initialItem: sigTypes.keys.toList().indexOf(sigType),
            items: sigTypes.values.toList())
        .then((value) {
      if (value != null) {
        sigType =
            sigTypes.keys.toList()[sigTypes.values.toList().indexOf(value)];
        sigTypeController.text = value;
        rebuildUi();
      }
    });
  }
}
