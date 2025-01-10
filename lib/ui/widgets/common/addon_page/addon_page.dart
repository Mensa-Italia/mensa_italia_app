import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
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
  Widget builder(BuildContext context, AddonPageModel viewModel, Widget? child) {
    return getCustomScrollViewPlatform(
      slivers: [
        getAppBarSliverPlatform(
          title: "views.addons.title".tr(),
          searchBarActions: SearchBarActions(
            onChanged: viewModel.search,
            controller: viewModel.searchController,
            onSubmitted: viewModel.search,
            hintText: "views.addons.search.textfield.hint".tr(),
          ),
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        if (viewModel.searchText.isEmpty)
          SliverToBoxAdapter(
            child: Padding(
              key: const ValueKey("Officials:Title"),
              padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(bottom: 10),
              child: Text(
                "views.addons.subtitle.officials".tr(),
                style: TextStyle(
                  fontSize: 18,
                ),
              ),
            ),
          ),
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          sliver: SliverGrid(
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 16 / 9,
            ),
            delegate: SliverChildListDelegate.fixed(
              [
                if (viewModel.isSearching("contacts"))
                  _InternalAddonButton(
                    key: const ValueKey("Internal:Contacts"),
                    id: "contacts",
                    name: "addons.contacts.title".tr(),
                    description: "addons.contacts.description".tr(),
                    icon: const Icon(EneftyIcons.bookmark_outline, color: kcPrimaryColor, size: 30),
                    onTap: viewModel.openContacts,
                  ),
                if (viewModel.isSearching("deals"))
                  _InternalAddonButton(
                    key: const ValueKey("Internal:Deals"),
                    id: "deals",
                    name: "addons.deals.title".tr(),
                    description: "addons.deals.description".tr(),
                    icon: const Icon(EneftyIcons.moneys_outline, color: kcPrimaryColor, size: 30),
                    onTap: viewModel.openDeals,
                  ),
                if (viewModel.isSearching("documents"))
                  _InternalAddonButton(
                    key: const ValueKey("Internal:Documents"),
                    id: "documents",
                    name: "addons.documents.title".tr(),
                    description: "addons.documents.description".tr(),
                    icon: const Icon(EneftyIcons.document_cloud_outline, color: kcPrimaryColor, size: 30),
                    onTap: viewModel.openDocuments,
                  ),
                if (viewModel.isSearching("tableport"))
                  _InternalAddonButton(
                    key: const ValueKey("Internal:Tableport"),
                    id: "tableport",
                    name: "addons.tableport.title".tr(),
                    description: "addons.tableport.description".tr(),
                    icon: const Icon(EneftyIcons.global_outline, color: kcPrimaryColor, size: 30),
                    onTap: viewModel.openTableport,
                  ),
                if (viewModel.allowTestMakerAddon() && viewModel.isSearching("testmakers"))
                  _InternalAddonButton(
                    key: const ValueKey("Internal:TestMakers"),
                    id: "testmakers",
                    name: "addons.testmakers.title".tr(),
                    description: "addons.testmakers.description".tr(),
                    icon: const Icon(EneftyIcons.teacher_outline, color: kcPrimaryColor, size: 30),
                    onTap: viewModel.openTestMakers,
                  ),
              ],
            ),
          ),
        ),
        if (viewModel.addons.isNotEmpty && viewModel.searchText.isEmpty)
          SliverToBoxAdapter(
            child: Padding(
              key: const ValueKey("Verified:Title"),
              padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(bottom: 10, top: 30),
              child: Text(
                "views.addons.subtitle.verified".tr(),
                style: TextStyle(
                  fontSize: 18,
                ),
              ),
            ),
          ),
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 10),
          sliver: SliverGrid(
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2, // Due colonne
              mainAxisSpacing: 10, // Spaziatura tra le righe
              crossAxisSpacing: 10, // Spaziatura tra le colonne
              childAspectRatio: 16 / 9, // Proporzione larghezza/altezza
            ),
            delegate: SliverChildListDelegate.fixed(
              [
                ...viewModel.addons.map((addon) {
                  return _ExternalAddonButton(
                    key: ValueKey(addon.id),
                    addon: addon,
                  );
                }),
              ],
            ),
          ),
        ),
        const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  AddonPageModel viewModelBuilder(BuildContext context) => AddonPageModel();
}

class _ExternalAddonButton extends ViewModelWidget<AddonPageModel> {
  final AddonModel addon;

  const _ExternalAddonButton({super.key, required this.addon});
  @override
  Widget build(BuildContext context, AddonPageModel viewModel) {
    return GestureDetector(
      onTap: () => viewModel.openAddon(addon),
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          color: Colors.white,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Container(
                    height: 40,
                    width: 40,
                    alignment: Alignment.topLeft,
                    padding: const EdgeInsets.all(15).copyWith(bottom: 0, top: 0),
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
                IconButton(
                  onPressed: () => viewModel.onStarTappedExternal(addon),
                  icon: Icon(
                    viewModel.getStarIconExternal(addon),
                    color: Theme.of(context).textTheme.titleLarge!.color,
                    size: 18,
                  ),
                ),
              ],
            ),
            Expanded(
              child: Container(
                alignment: Alignment.bottomLeft,
                padding: const EdgeInsets.all(15).copyWith(top: 0, bottom: 10),
                child: Text(
                  addon.name,
                  textAlign: TextAlign.start,
                  style: const TextStyle(
                    height: 1,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );

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
                    memCacheHeight: 180,
                    memCacheWidth: 180,
                    maxHeightDiskCache: 180,
                    maxWidthDiskCache: 180,
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
  final String id;
  final String name;
  final String description;
  final Widget icon;
  final Function() onTap;

  const _InternalAddonButton({super.key, required this.id, required this.name, required this.description, required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context, AddonPageModel viewModel) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(20),
          color: Colors.white,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(
                  child: Container(
                    alignment: Alignment.topLeft,
                    padding: const EdgeInsets.all(15).copyWith(bottom: 0),
                    child: icon,
                  ),
                ),
                IconButton(
                  onPressed: () => viewModel.onStarTappedInternal(id),
                  icon: Icon(
                    viewModel.getStarIconInternal(id),
                    color: Theme.of(context).textTheme.titleLarge!.color,
                    size: 18,
                  ),
                ),
              ],
            ),
            Expanded(
              child: Container(
                alignment: Alignment.bottomLeft,
                padding: const EdgeInsets.all(15).copyWith(top: 0, bottom: 10),
                child: Text(
                  name,
                  textAlign: TextAlign.start,
                  style: const TextStyle(
                    height: 1,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
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
                  viewModel.getStarIconInternal(id),
                  color: Colors.grey,
                  size: 35,
                ),
                onPressed: () => viewModel.onStarTappedInternal(id),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
