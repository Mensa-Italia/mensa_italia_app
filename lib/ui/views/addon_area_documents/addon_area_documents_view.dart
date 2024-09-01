import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
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
            title: "Documents",
            previousPageTitle: "Addons",
            searchBarActions: SearchBarActions(
              onChanged: viewModel.search,
              controller: viewModel.searchController,
              onSubmitted: viewModel.search,
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
                  key: ValueKey(viewModel.documents[index].link),
                  onTap: viewModel.onTap(viewModel.documents[index]),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 10),
                  dense: true,
                  visualDensity: VisualDensity.compact,
                  leading: CachedNetworkImage(
                    imageUrl: viewModel.documents[index].image,
                    width: 30,
                    height: 30,
                    memCacheHeight: 90,
                    memCacheWidth: 90,
                    maxHeightDiskCache: 90,
                    maxWidthDiskCache: 90,
                    fit: BoxFit.cover,
                    imageBuilder: (context, imageProvider) => Container(
                      width: 30,
                      height: 30,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(300),
                        image: DecorationImage(
                          image: imageProvider,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                  ),
                  title: Text(viewModel.documents[index].description),
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
