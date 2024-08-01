import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:webview_flutter/webview_flutter.dart';

import 'generic_webview_viewmodel.dart';

class GenericWebviewView extends StackedView<GenericWebviewViewModel> {
  final String url;
  const GenericWebviewView({Key? key, required this.url}) : super(key: key);

  @override
  Widget builder(
      BuildContext context, GenericWebviewViewModel viewModel, Widget? child) {
    return Scaffold(
      body: SafeArea(
        child: viewModel.controller == null
            ? Container()
            : WebViewWidget(
                controller: viewModel.controller!,
              ),
      ),
    );
  }

  @override
  GenericWebviewViewModel viewModelBuilder(BuildContext context) =>
      GenericWebviewViewModel(url: url);
}
