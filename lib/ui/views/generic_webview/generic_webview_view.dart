import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';
import 'package:webview_flutter/webview_flutter.dart';

import 'generic_webview_viewmodel.dart';

class GenericWebviewView extends StackedView<GenericWebviewViewModel> {
  final String title;
  final String previousPageTitle;
  final String url;
  const GenericWebviewView(
      {super.key,
      required this.url,
      required this.title,
      required this.previousPageTitle});

  @override
  Widget builder(
      BuildContext context, GenericWebviewViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: title,
        previousPageTitle: previousPageTitle,
      ),
      body: SafeArea(
        child: viewModel.controller == null
            ? Container()
            : Opacity(
                opacity: viewModel.wholeOpacity,
                child: WebViewWidget(
                  controller: viewModel.controller!,
                ),
              ),
      ),
    );
  }

  @override
  GenericWebviewViewModel viewModelBuilder(BuildContext context) =>
      GenericWebviewViewModel(url: url);
}
