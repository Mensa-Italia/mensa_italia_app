import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/sigs_page/sigs_page_view.dart';
import 'package:stacked/stacked.dart';

import 'addon_area_documents_viewmodel.dart';

class AddonAreaDocumentsView extends StackedView<AddonAreaDocumentsViewModel> {
  const AddonAreaDocumentsView({super.key});

  @override
  Widget builder(BuildContext context, AddonAreaDocumentsViewModel viewModel, Widget? child) {
    return Scaffold(
      body: getCustomScrollViewPlatform(
        controller: viewModel.scrollController,
        slivers: [
          getAppBarSliverPlatform(
            title: "views.addons.documents.title".tr(),
            previousPageTitle: "addons.documents.view.previouspagetitle".tr(),
            searchBarActions: SearchBarActions(
              onChanged: viewModel.search,
              controller: viewModel.searchController,
              onSubmitted: viewModel.search,
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 0),
            sliver: SliverToBoxAdapter(
              child: SizedBox(
                height: 50,
                child: ListView(
                  padding: const EdgeInsets.symmetric(horizontal: 10),
                  scrollDirection: Axis.horizontal,
                  children: List.generate(viewModel.categories.length, (index) {
                    final category = viewModel.categories[index];
                    return Center(
                      child: ChipWidget(
                        label: "addons.documents.view.category.$category".tr(),
                        isActived: viewModel.isActived(index),
                        onTap: viewModel.selectChip(index),
                      ),
                    );
                  }),
                ),
              ),
            ),
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          if (viewModel.documents.isEmpty)
            const SliverFillRemaining(
              child: SizedBox(
                height: 100,
                child: Center(
                  child: Text("No data found"),
                ),
              ),
            )
          else
            SliverList.separated(
              itemCount: viewModel.documents.length,
              itemBuilder: (context, index) {
                return ListTile(
                  key: ValueKey(viewModel.documents[index].file),
                  onTap: viewModel.onTap(viewModel.documents[index]),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 10),
                  dense: true,
                  visualDensity: VisualDensity.compact,
                  leading: Icon(viewModel.getIconBasedOnCategory(viewModel.documents[index].category)),
                  title: Text(viewModel.documents[index].name),
                );
              },
              separatorBuilder: (context, index) => const Divider(),
            ),
          const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
        ],
      ),
    );
  }

  @override
  AddonAreaDocumentsViewModel viewModelBuilder(BuildContext context) => AddonAreaDocumentsViewModel();
}
