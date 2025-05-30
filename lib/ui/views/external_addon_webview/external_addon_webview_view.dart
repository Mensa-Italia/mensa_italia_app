import 'dart:io';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'external_addon_webview_viewmodel.dart';

class ExternalAddonWebviewView
    extends StackedView<ExternalAddonWebviewViewModel> {
  final String addonID;
  final String addonURL;
  const ExternalAddonWebviewView(
      {super.key, required this.addonID, required this.addonURL});

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
      ExternalAddonWebviewViewModel(addonID, addonURL);

  Widget iosFixer(Widget child, ExternalAddonWebviewViewModel viewModel) {
    return child;
    if (!Platform.isIOS) {
      return child;
    } else {
      return GestureDetector(
        behavior: HitTestBehavior.translucent,
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
