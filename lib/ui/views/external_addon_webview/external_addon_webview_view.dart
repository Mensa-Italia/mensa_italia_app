import 'dart:io';

import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'external_addon_webview_viewmodel.dart';

class ExternalAddonWebviewView
    extends StackedView<ExternalAddonWebviewViewModel> {
  final String addonID;
  const ExternalAddonWebviewView({Key? key, required this.addonID})
      : super(key: key);

  @override
  Widget builder(BuildContext context, ExternalAddonWebviewViewModel viewModel,
      Widget? child) {
    return PopScope(
      canPop: viewModel.willPopCallback(),
      onPopInvoked: viewModel.onPopInvoked,
      child: iosFixer(
          Scaffold(
            body: SafeArea(
              child: viewModel.controller == null
                  ? Container()
                  : WebViewWidget(
                      controller: viewModel.controller!,
                    ),
            ),
          ),
          viewModel),
    );
  }

  @override
  ExternalAddonWebviewViewModel viewModelBuilder(BuildContext context) =>
      ExternalAddonWebviewViewModel(addonID);

  Widget iosFixer(Widget child, ExternalAddonWebviewViewModel viewModel) {
    if (!Platform.isIOS) {
      return child;
    } else {
      return GestureDetector(
        onHorizontalDragUpdate: (details) {
          if (details.primaryDelta! > 10) {
            viewModel.onPopInvoked(true);
          }
        },
        child: child,
      );
    }
  }
}
