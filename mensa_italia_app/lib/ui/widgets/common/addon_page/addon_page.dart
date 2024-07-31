import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/app_strings.dart';
import 'package:stacked/stacked.dart';

import 'addon_page_model.dart';

class AddonPage extends StackedView<AddonPageModel> {
  const AddonPage({super.key});

  @override
  Widget builder(BuildContext context, AddonPageModel viewModel, Widget? child) {
    return CustomScrollView(
      slivers: [
        CupertinoSliverNavigationBar(
          largeTitle: const Text('Addons', style: TextStyle(fontWeight: FontWeight.w900)),
          middle: const Text('Addons', style: TextStyle(fontWeight: FontWeight.bold)),
          alwaysShowMiddle: false,
          backgroundColor: Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
          border: null,
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.list(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 10),
              child: const Text(
                "Officials",
                style: TextStyle(
                  fontSize: 18,
                ),
              ),
            ),
            _InternalAddonButton(
              name: "Phonebook",
              description: "You Mensa Italia contacts, you can find any contact you need!",
              icon: const Icon(EneftyIcons.bookmark_outline, color: kcPrimaryColor, size: 40),
              onTap: viewModel.openContacts,
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 30),
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
        const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
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
      onTap: () => viewModel.openAddon(),
      child: Container(
        height: 100,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              width: 80,
              height: 80,
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.2),
                    blurRadius: 3,
                    offset: const Offset(0, 3),
                  ),
                ],
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
            const Center(
              child: Icon(EneftyIcons.star_outline, color: Colors.grey, size: 35),
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

  const _InternalAddonButton({Key? key, required this.name, required this.description, required this.icon, required this.onTap}) : super(key: key);

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
            Container(
              width: 80,
              height: 80,
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.2),
                    blurRadius: 3,
                    offset: const Offset(0, 3),
                  ),
                ],
              ),
              alignment: Alignment.center,
              child: AspectRatio(
                aspectRatio: 1,
                child: icon,
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
            const Center(
              child: Icon(EneftyIcons.star_outline, color: Colors.grey, size: 35),
            ),
          ],
        ),
      ),
    );
  }
}
