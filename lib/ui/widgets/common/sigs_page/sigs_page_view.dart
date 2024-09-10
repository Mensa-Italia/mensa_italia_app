import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
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
          title: "Community",
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
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          sliver: SliverToBoxAdapter(
            child: SizedBox(
              height: 50,
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  Center(
                      child: ChipWidget(
                          label: "All",
                          isActived: viewModel.isActived(0),
                          onTap: viewModel.selectChip(0))),
                  Center(
                      child: ChipWidget(
                          label: "Local groups",
                          isActived: viewModel.isActived(1),
                          onTap: viewModel.selectChip(1))),
                  Center(
                      child: ChipWidget(
                          label: "SIGs",
                          isActived: viewModel.isActived(2),
                          onTap: viewModel.selectChip(2))),
                  Center(
                      child: ChipWidget(
                          label: "Chats",
                          isActived: viewModel.isActived(3),
                          onTap: viewModel.selectChip(3))),
                ],
              ),
            ),
          ),
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        viewModel.sigs.isEmpty
            ? const SliverToBoxAdapter(
                child: Center(
                  child: Text("No community found"),
                ),
              )
            : SliverList.builder(
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

class ChipWidget extends StatelessWidget {
  const ChipWidget(
      {super.key,
      required this.label,
      this.isActived = false,
      required this.onTap});

  final Function() onTap;
  final bool isActived;
  final String label;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        margin: const EdgeInsets.only(right: 5),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: isActived ? kcMediumGrey : kcVeryLightGrey,
          borderRadius: BorderRadius.circular(20),
        ),
        child: Text(label,
            style: TextStyle(color: isActived ? Colors.white : Colors.black)),
      ),
    );
  }
}
