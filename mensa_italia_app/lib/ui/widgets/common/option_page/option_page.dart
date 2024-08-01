import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'option_page_model.dart';

class OptionPage extends StackedView<OptionPageModel> {
  const OptionPage({super.key});

  @override
  Widget builder(BuildContext context, OptionPageModel viewModel, Widget? child) {
    return CustomScrollView(
      slivers: [
        CupertinoSliverNavigationBar(
          largeTitle: const Text('Settings', style: TextStyle(fontWeight: FontWeight.w900)),
          middle: const Text('Settings', style: TextStyle(fontWeight: FontWeight.bold)),
          alwaysShowMiddle: false,
          backgroundColor: Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
          border: null,
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.list(
          children: [
            ListTile(
              onTap: viewModel.logout,
              title: Text(
                "Logout",
              ),
              leading: Icon(EneftyIcons.logout_outline),
            ),
          ],
        ),
        const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  OptionPageModel viewModelBuilder(BuildContext context) => OptionPageModel();
}
