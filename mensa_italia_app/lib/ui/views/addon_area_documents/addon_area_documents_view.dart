import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'addon_area_documents_viewmodel.dart';

class AddonAreaDocumentsView extends StackedView<AddonAreaDocumentsViewModel> {
  const AddonAreaDocumentsView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    AddonAreaDocumentsViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      body: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        controller: viewModel.scrollController,
        anchor: 0.06,
        slivers: [
          CupertinoSliverNavigationBar(
            largeTitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Documents',
                  style: TextStyle(fontWeight: FontWeight.w900),
                ),
                Container(
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
            previousPageTitle: "Addons",
            backgroundColor:
                Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
            border: null,
            middle: const Text(
              'Documents',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            alwaysShowMiddle: false,
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
                  onTap: viewModel.onTap(viewModel.documents[index]),
                  contentPadding: const EdgeInsets.symmetric(horizontal: 10),
                  dense: true,
                  visualDensity: VisualDensity.compact,
                  leading: CachedNetworkImage(
                    imageUrl: viewModel.documents[index].image,
                    width: 30,
                    height: 30,
                    imageBuilder: (context, imageProvider) => Container(
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
          const SliverSafeArea(
              sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
        ],
      ),
    );
  }

  @override
  AddonAreaDocumentsViewModel viewModelBuilder(BuildContext context) =>
      AddonAreaDocumentsViewModel();
}
