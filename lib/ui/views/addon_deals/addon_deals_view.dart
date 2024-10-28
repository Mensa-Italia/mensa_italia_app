import 'package:easy_localization/easy_localization.dart';
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
        controller: viewModel.scrollController,
        slivers: [
          getAppBarSliverPlatform(
            title: "addons.deals.view.title".tr(),
            previousPageTitle: "addons.deals.view.previouspagetitle".tr(),
            searchBarActions: SearchBarActions(
              onChanged: viewModel.onChanged,
              controller: viewModel.controller,
              onSubmitted: viewModel.onSubmitted,
              hintText: "addons.deals.search.textfield".tr(),
            ),
            trailings: [
              if (viewModel.allowControlDeals())
                CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.tapAddDeal,
                  child: const Icon(EneftyIcons.add_circle_bold, color: kcPrimaryColor),
                ),
            ],
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          SliverList.separated(
            itemCount: viewModel.deals.length,
            itemBuilder: (context, index) {
              return _DealTile(
                key: ValueKey(viewModel.deals[index].id),
                deal: viewModel.deals[index],
                onTap: viewModel.tapOnDeal(index),
                onLongPress: viewModel.onLongPress(index),
              );
            },
            separatorBuilder: (context, index) => Divider(height: 0, endIndent: 0, indent: 0, key: ValueKey(index)),
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
  final VoidCallback onLongPress;

  const _DealTile({super.key, required this.deal, required this.onTap, required this.onLongPress});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      title: Text(deal.name),
      subtitle: Text(deal.commercialSector),
      onTap: onTap,
      onLongPress: onLongPress,
    );
  }
}
