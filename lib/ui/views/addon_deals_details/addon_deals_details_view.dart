import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:stacked/stacked.dart';

import 'addon_deals_details_viewmodel.dart';

class AddonDealsDetailsView extends StackedView<AddonDealsDetailsViewModel> {
  final DealModel deal;
  const AddonDealsDetailsView({super.key, required this.deal});

  @override
  Widget builder(
    BuildContext context,
    AddonDealsDetailsViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      body: Container(
        padding: const EdgeInsets.only(left: 25.0, right: 25.0),
      ),
    );
  }

  @override
  AddonDealsDetailsViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      AddonDealsDetailsViewModel();
}
