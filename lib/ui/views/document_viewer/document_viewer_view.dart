import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:syncfusion_flutter_pdfviewer/pdfviewer.dart';
import 'document_viewer_viewmodel.dart';

class DocumentViewerView extends StackedView<DocumentViewerViewModel> {
  final String downlaodUrl;
  const DocumentViewerView({Key? key, required this.downlaodUrl}) : super(key: key);

  @override
  Widget builder(BuildContext context, DocumentViewerViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: const CupertinoNavigationBar(
        middle: Text("Document"),
        previousPageTitle: "Documents",
      ),
      body: viewModel.hasFailed
          ? failedInfos(viewModel)
          : viewModel.genericFile == null
              ? Container(
                  child: const Center(
                    child: CircularProgressIndicator(),
                  ),
                )
              : SfPdfViewer.file(
                  viewModel.genericFile!,
                  onDocumentLoadFailed: viewModel.onDocumentLoadFailed,
                  enableTextSelection: false,
                ),
    );
  }

  Widget failedInfos(DocumentViewerViewModel viewModel) {
    return Container(
      margin: const EdgeInsets.all(20),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Icon(
            Icons.error,
            size: 50,
            color: Colors.red,
          ),
          SizedBox(height: 20),
          const Text(
            "Failed to load document, probably because this is not a PDF file. Try to get it from the web browser.",
            style: TextStyle(color: Colors.red),
            textAlign: TextAlign.center,
          ),
          SizedBox(height: 40),
        ],
      ),
    );
  }

  @override
  DocumentViewerViewModel viewModelBuilder(BuildContext context) => DocumentViewerViewModel(downloadUrl: downlaodUrl);
}
