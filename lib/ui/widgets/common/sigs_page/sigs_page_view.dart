import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/sig_tile/sig_tile.dart';
import 'package:stacked/stacked.dart';

import 'sigs_page_viewmodel.dart';

class SigsPage extends StackedView<SigsPageModel> {
  const SigsPage({super.key});

  @override
  Widget builder(BuildContext context, SigsPageModel viewModel, Widget? child) {
    return getCustomScrollViewPlatform(
      slivers: [
        getAppBarSliverPlatform(
          title: "SiGs",
          searchBarActions: SearchBarActions(
            onChanged: viewModel.search,
            controller: viewModel.searchController,
            onSubmitted: viewModel.search,
          ),
          leading: (viewModel.allowControlSigs())
              ? CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.onTapAddSig,
                  child: const Icon(
                    EneftyIcons.add_circle_bold,
                    color: kcPrimaryColor,
                  ),
                )
              : null,
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.builder(
          itemCount: viewModel.sigs.length,
          itemBuilder: (context, index) {
            final sig = viewModel.sigs[index];
            return SigTile(
              key: ValueKey(sig.id),
              sig: sig,
              onTap: viewModel.onTapOnSIG(sig),
              onLongTap: (viewModel.allowControlSigs())
                  ? viewModel.onLongTapEditSig(sig)
                  : null,
            );
          },
        ),
        const SliverSafeArea(
            sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  SigsPageModel viewModelBuilder(BuildContext context) => SigsPageModel();
}
