import 'package:cached_network_image/cached_network_image.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
import 'package:stacked/stacked.dart';

import 'addon_page_model.dart';

class AddonPage extends StackedView<AddonPageModel> {
  const AddonPage({super.key});

  @override
  Widget builder(
      BuildContext context, AddonPageModel viewModel, Widget? child) {
    return getCustomScrollViewPlatform(
      slivers: [
        getAppBarSliverPlatform(
          title: "Addons",
          searchBarActions: SearchBarActions(
            onChanged: viewModel.search,
            controller: viewModel.searchController,
            onSubmitted: viewModel.search,
          ),
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.list(
          children: [
            if (viewModel.addons.isNotEmpty && viewModel.searchText.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20)
                    .copyWith(top: 10),
                child: const Text(
                  "Officials",
                  style: TextStyle(
                    fontSize: 18,
                  ),
                ),
              ),
            if (viewModel.isSearching("contacts"))
              _InternalAddonButton(
                name: "Contacts",
                description:
                    "Your Mensa Italia contacts, you can find any contact you need!",
                icon: const Icon(EneftyIcons.bookmark_outline,
                    color: kcPrimaryColor, size: 40),
                onTap: viewModel.openContacts,
              ),
            if (viewModel.isSearching("deals"))
              _InternalAddonButton(
                name: "Deals",
                description: "Deals and discounts for Mensa Italia members",
                icon: const Icon(EneftyIcons.moneys_outline,
                    color: kcPrimaryColor, size: 40),
                onTap: viewModel.openDeals,
              ),
            if (viewModel.isSearching("documents"))
              _InternalAddonButton(
                name: "Documents",
                description: "Official documents of Mensa Italia",
                icon: const Icon(EneftyIcons.document_cloud_outline,
                    color: kcPrimaryColor, size: 40),
                onTap: viewModel.openDocuments,
              ),
            if (viewModel.allowTestMakerAddon() &&
                viewModel.isSearching("testmakers"))
              _InternalAddonButton(
                name: "TestMakers",
                description:
                    "You see this because you're one of the test makers!",
                icon: const Icon(EneftyIcons.teacher_outline,
                    color: kcPrimaryColor, size: 40),
                onTap: viewModel.openTestMakers,
              ),
            if (viewModel.addons.isNotEmpty && viewModel.searchText.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20)
                    .copyWith(top: 30),
                child: const Text(
                  "Verified",
                  style: TextStyle(
                    fontSize: 18,
                  ),
                ),
              ),
            ...viewModel.addons.map((addon) {
              return _ExternalAddonButton(addon: addon);
            }).toList(),
          ],
        ),
        const SliverSafeArea(
            sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  AddonPageModel viewModelBuilder(BuildContext context) => AddonPageModel();
}

class _ExternalAddonButton extends ViewModelWidget<AddonPageModel> {
  final AddonModel addon;

  const _ExternalAddonButton({Key? key, required this.addon}) : super(key: key);
  @override
  Widget build(BuildContext context, AddonPageModel viewModel) {
    return GestureDetector(
      onTap: () => viewModel.openAddon(addon),
      child: Container(
        height: 100,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Material(
              color: Colors.transparent,
              elevation: 1,
              borderRadius: BorderRadius.circular(20),
              child: Container(
                width: 80,
                height: 80,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: AspectRatio(
                  aspectRatio: 1,
                  child: CachedNetworkImage(
                    imageUrl: addon.icon,
                    fit: BoxFit.cover,
                    color: kcPrimaryColor,
                  ),
                ),
              ),
            ),
            const SizedBox(
              width: 5,
            ),
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(10),
                height: 80,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      addon.name,
                      style: const TextStyle(
                        fontSize: 20,
                        color: kcPrimaryColor,
                        height: 1,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    Flexible(
                      child: Text(
                        addon.description,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 14,
                          height: 1,
                          color: Colors.grey,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Center(
              child: IconButton(
                onPressed: () => viewModel.onStarTappedExternal(addon),
                icon: Icon(
                  viewModel.getStarIconExternal(addon),
                  color: Colors.grey,
                  size: 35,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InternalAddonButton extends ViewModelWidget<AddonPageModel> {
  final String name;
  final String description;
  final Widget icon;
  final Function() onTap;

  const _InternalAddonButton(
      {Key? key,
      required this.name,
      required this.description,
      required this.icon,
      required this.onTap})
      : super(key: key);

  @override
  Widget build(BuildContext context, AddonPageModel viewModel) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        height: 100,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Material(
              color: Colors.transparent,
              elevation: 1,
              borderRadius: BorderRadius.circular(20),
              child: Container(
                width: 80,
                height: 80,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(20),
                ),
                alignment: Alignment.center,
                child: AspectRatio(
                  aspectRatio: 1,
                  child: icon,
                ),
              ),
            ),
            const SizedBox(
              width: 5,
            ),
            Expanded(
              child: Container(
                padding: const EdgeInsets.all(10),
                height: 80,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      name,
                      style: const TextStyle(
                        fontSize: 20,
                        color: kcPrimaryColor,
                        height: 1,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    Flexible(
                      child: Text(
                        description,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 14,
                          height: 1,
                          color: Colors.grey,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            Center(
              child: IconButton(
                icon: Icon(
                  viewModel.getStarIconInternal(name),
                  color: Colors.grey,
                  size: 35,
                ),
                onPressed: () => viewModel.onStarTappedInternal(name),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
