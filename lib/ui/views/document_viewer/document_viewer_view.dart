import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'document_viewer_viewmodel.dart';

class DocumentViewerView extends StackedView<DocumentViewerViewModel> {
  const DocumentViewerView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    DocumentViewerViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.background,
      body: Container(
        padding: const EdgeInsets.only(left: 25.0, right: 25.0),
      ),
    );
  }

  @override
  DocumentViewerViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      DocumentViewerViewModel();
}
