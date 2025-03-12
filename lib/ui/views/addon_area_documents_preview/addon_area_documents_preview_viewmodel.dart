import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/document.dart';
import 'package:mensa_italia_app/model/document_elaborated.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonAreaDocumentsPreviewViewModel extends MasterModel {
  final DocumentModel document;
  DocumentElaboratedModel? documentElaboratedModel;
  ScrollController scrollController = ScrollController();

  AddonAreaDocumentsPreviewViewModel({required this.document}) {
    setBusy(true);
    Api().getDocumentElaboratedData(document.elaborated).then((value) {
      documentElaboratedModel = value;
      setBusy(false);
    });
  }

  void onTapViewOriginal() {
    navigationService.navigateToDocumentViewerView(
      downlaodUrl: document.file,
      title: "Document",
      previousPageTitle: "views.addons.documents_resume.title".tr(),
    );
  }
}
