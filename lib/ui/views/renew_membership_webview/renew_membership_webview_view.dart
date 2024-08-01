import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';
import 'package:webview_flutter/webview_flutter.dart';

import 'renew_membership_webview_viewmodel.dart';

class RenewMembershipWebviewView
    extends StackedView<RenewMembershipWebviewViewModel> {
  final String url;
  const RenewMembershipWebviewView({Key? key, required this.url})
      : super(key: key);

  @override
  Widget builder(BuildContext context,
      RenewMembershipWebviewViewModel viewModel, Widget? child) {
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
  RenewMembershipWebviewViewModel viewModelBuilder(BuildContext context) =>
      RenewMembershipWebviewViewModel(url: url);
}
