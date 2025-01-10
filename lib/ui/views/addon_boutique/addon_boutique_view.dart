import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'addon_boutique_viewmodel.dart';

class AddonBoutiqueView extends StackedView<AddonBoutiqueViewModel> {
  const AddonBoutiqueView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    AddonBoutiqueViewModel viewModel,
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
  AddonBoutiqueViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      AddonBoutiqueViewModel();
}
