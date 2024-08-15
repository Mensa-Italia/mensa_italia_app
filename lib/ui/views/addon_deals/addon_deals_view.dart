import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
import 'package:stacked/stacked.dart';

import 'addon_deals_viewmodel.dart';

class AddonDealsView extends StackedView<AddonDealsViewModel> {
  const AddonDealsView({super.key});

  @override
  Widget builder(
    BuildContext context,
    AddonDealsViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      body: getCustomScrollViewPlatform(
        slivers: [
          getAppBarSliverPlatform(
            title: "Deals",
            previousPageTitle: "Addons",
            searchBarActions: SearchBarActions(
              onChanged: viewModel.onChanged,
              controller: viewModel.controller,
              onSubmitted: viewModel.onSubmitted,
            ),
            trailings: [
              CupertinoButton(
                padding: EdgeInsets.zero,
                child: const Icon(EneftyIcons.add_circle_bold, color: kcPrimaryColor),
                onPressed: () {},
              ),
            ],
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          SliverList.separated(
            itemCount: viewModel.deals.length,
            itemBuilder: (context, index) {
              return _DealTile(
                deal: viewModel.deals[index],
                onTap: viewModel.tapOnDeal(index),
              );
            },
            separatorBuilder: (context, index) => const Divider(height: 0, endIndent: 0, indent: 0),
          ),
          const SliverSafeArea(
            sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10)),
          ),
        ],
      ),
    );
  }

  @override
  AddonDealsViewModel viewModelBuilder(BuildContext context) => AddonDealsViewModel();
}

class _DealTile extends StatelessWidget {
  final DealModel deal;
  final VoidCallback onTap;

  const _DealTile({required this.deal, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(deal.name),
      subtitle: Text(deal.commercialSector),
      onTap: onTap,
    );
  }
}
