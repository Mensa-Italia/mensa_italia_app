import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
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
          backgroundColor:
              Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
          border: null,
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
            return _SigTile(sig: viewModel.sigs[index]);
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

class _SigTile extends ViewModelWidget<SigsPageModel> {
  final SigModel sig;

  const _SigTile({Key? key, required this.sig}) : super(key: key);

  @override
  Widget build(BuildContext context, SigsPageModel viewModel) {
    return GestureDetector(
      onTap: viewModel.onTapOnSIG(sig),
      child: Padding(
        padding: const EdgeInsets.all(8.0),
        child: AspectRatio(
          aspectRatio: 1528 / 603,
          child: Container(
            clipBehavior: Clip.antiAlias,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(30),
              gradient: const LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  kcPrimaryColor,
                  kcPrimaryColorDark,
                ],
              ),
            ),
            child: sig.image.isEmpty
                ? Center(
                    child: Text(
                      sig.name,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  )
                : CachedNetworkImage(imageUrl: sig.image, fit: BoxFit.cover),
          ),
        ),
      ),
    );
  }
}
