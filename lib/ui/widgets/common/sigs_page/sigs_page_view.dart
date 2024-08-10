import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/widgets/common/sig_tile/sig_tile.dart';
import 'package:stacked/stacked.dart';

import 'sigs_page_viewmodel.dart';

class SigsPage extends StackedView<SigsPageModel> {
  const SigsPage({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, SigsPageModel viewModel, Widget? child) {
    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      controller: viewModel.scrollController,
      anchor: 0.06,
      slivers: [
        CupertinoSliverNavigationBar(
          largeTitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'SiGs',
                style: TextStyle(fontWeight: FontWeight.w900),
              ),
              SizedBox(
                height: 40,
                child: Padding(
                  padding: const EdgeInsets.only(right: 15, top: 3),
                  child: CupertinoSearchTextField(
                    onChanged: viewModel.search,
                    controller: viewModel.searchController,
                    prefixIcon: const Icon(CupertinoIcons.search),
                    onSubmitted: viewModel.search,
                  ),
                ),
              ),
            ],
          ),
          stretch: true,
          backgroundColor:
              Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
          border: null,
          leading: viewModel.allowControlSigs()
              ? CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.onTapAddSig,
                  child: const Icon(
                    CupertinoIcons.add_circled_solid,
                    color: kcPrimaryColor,
                  ),
                )
              : null,
          middle: const Text(
            'SiGs',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          alwaysShowMiddle: false,
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.builder(
          itemCount: viewModel.sigs.length,
          itemBuilder: (context, index) {
            final sig = viewModel.sigs[index];
            return SigTile(
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
