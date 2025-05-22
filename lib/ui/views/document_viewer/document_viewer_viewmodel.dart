import 'dart:io';

import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:stacked/stacked.dart';
import 'package:syncfusion_flutter_pdfviewer/src/control/pdfviewer_callback_details.dart';

class DocumentViewerViewModel extends BaseViewModel {
  File? genericFile;
  bool hasFailed = false;
  final String downloadUrl;

  DocumentViewerViewModel({this.downloadUrl = ""}) {
    ScraperApi().getFile(downloadUrl).then((value) {
      genericFile = value;
      rebuildUi();
    });
  }

  void onDocumentLoadFailed(PdfDocumentLoadFailedDetails details) {
    hasFailed = true;
    rebuildUi();
  }
}
