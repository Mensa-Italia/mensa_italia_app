import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:flutter_flip_card/controllers/flip_card_controllers.dart';
import 'package:flutter_flip_card/flipcard/flip_card.dart';
import 'package:flutter_flip_card/modal/flip_side.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/views/login/login_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/front_card_shine/front_card_shine.dart';
import 'package:stacked/stacked.dart';

import 'membership_page_model.dart';

final internalAddonsList = ["Contacts", "TestMakers", "Documents", "Deals"];

class MembershipPage extends StackedView<MembershipPageModel> {
  const MembershipPage({super.key});

  @override
  Widget builder(BuildContext context, MembershipPageModel viewModel, Widget? child) {
    return ListView(
      padding: const EdgeInsets.all(0),
      children: [
        const SafeArea(
          bottom: false,
          child: SizedBox(),
        ),
        const SizedBox(
          height: 20,
        ),
        const _UserInfoTopBar(
          key: ValueKey("UserInfoTopBar"),
        ),
        RepaintBoundary(
          key: const ValueKey("MembershipCard"),
          child: _MembershipCard(),
        ),
        if (viewModel.nextEvent != null || viewModel.lastSig != null || viewModel.lastBlogPost != null) ...[
          Padding(
            padding: EdgeInsets.all(8.0),
            child: Text("views.home.subtitle.highlights".tr()),
          ),
          const _highlights(
            key: ValueKey("Highlights"),
          ),
        ],
        if (viewModel.favsAddons.isNotEmpty) ...[
          Padding(
            padding: EdgeInsets.all(8.0),
            child: Text("views.home.subtitle.favaddons".tr()),
          ),
          const _addons(
            key: ValueKey("FavouriteAddons"),
          ),
        ],
        const SafeArea(
          child: SizedBox(
            height: 20,
          ),
        ),
      ],
    );
  }

  @override
  MembershipPageModel viewModelBuilder(BuildContext context) => MembershipPageModel();
}

class _UserInfoTopBar extends ViewModelWidget<MembershipPageModel> {
  const _UserInfoTopBar({super.key});

  @override
  Widget build(BuildContext context, MembershipPageModel viewModel) {
    return RepaintBoundary(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 30),
        child: Row(
          children: [
            Text.rich(
              TextSpan(
                text: "views.home.hello".tr(
                  namedArgs: {
                    "name": viewModel.user.name.split(" ").first,
                  },
                ),
                style: const TextStyle(
                  fontSize: 16,
                  height: 1,
                ),
              ),
            ),
            const Expanded(child: SizedBox()),
            CircleAvatar(
              radius: 25,
              backgroundImage: CachedNetworkImageProvider(
                viewModel.user.avatar,
                maxHeight: 150,
                maxWidth: 150,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _MembershipCard extends ViewModelWidget<MembershipPageModel> {
  @override
  Widget build(BuildContext context, MembershipPageModel viewModel) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(30),
        child: AspectRatio(
          aspectRatio: 1.586,
          child: FlipCard(
            rotateSide: RotateSide.right,
            axis: FlipAxis.vertical,
            onTapFlipping: true,
            frontWidget: front(viewModel),
            backWidget: back(viewModel),
            controller: FlipCardController(),
          ),
        ),
      ),
    );
  }

  Widget front(MembershipPageModel viewModel) {
    return const FrontCardShine();
  }

  Widget back(MembershipPageModel viewModel) {
    var nomeProfilo = viewModel.user.name.replaceFirst(" ", "~~~").split("~~~");
    var ntessera = viewModel.user.id;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: kcPrimaryColor,
        borderRadius: BorderRadius.circular(20),
        image: DecorationImage(
          image: const AssetImage(
            "assets/images/backcard.jpg",
          ),
          fit: BoxFit.cover,
          colorFilter: ColorFilter.mode(Colors.white.withOpacity(0.3), BlendMode.srcATop),
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.2),
            blurRadius: 3,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: LayoutBuilder(builder: (BuildContext context, BoxConstraints constraints) {
        return SizedBox(
          height: constraints.maxHeight,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.max,
            children: <Widget>[
              Image.asset(
                "assets/images/lettering_horizzontal_white.png",
                width: constraints.maxWidth * 2 / 3,
                cacheHeight: 235,
                cacheWidth: 586,
              ),
              Expanded(
                child: Container(
                  margin: EdgeInsets.only(left: constraints.maxWidth * 2 / 7),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Expanded(
                        child: Builder(
                          builder: (_) {
                            if (nomeProfilo.isEmpty) {
                              const Column(mainAxisAlignment: MainAxisAlignment.start, crossAxisAlignment: CrossAxisAlignment.start, children: []);
                            }
                            return Column(
                              mainAxisAlignment: MainAxisAlignment.start,
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: List.generate(nomeProfilo.length, (i) {
                                return Expanded(
                                  child: AutoSizeText(nomeProfilo[i].toUpperCase().trim(), style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 20), textAlign: TextAlign.left, minFontSize: 0, maxLines: 1),
                                );
                              }),
                            );
                          },
                        ),
                      ),
                      Expanded(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.end,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: <Widget>[
                            Expanded(
                              child: Container(
                                alignment: Alignment.bottomLeft,
                                child: const AutoSizeText(
                                  "Tessera",
                                  style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                  ),
                                  minFontSize: 0,
                                ),
                              ),
                            ),
                            Expanded(
                              child: Container(
                                width: constraints.maxWidth - (constraints.maxWidth * 2 / 7),
                                alignment: Alignment.bottomLeft,
                                child: Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                  mainAxisSize: MainAxisSize.max,
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: <Widget>[
                                    AutoSizeText(ntessera, style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 20), minFontSize: 0),
                                    const AutoSizeText(
                                      "MENSA.IT",
                                      style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                                      minFontSize: 0,
                                    ),
                                  ],
                                ),
                              ),
                            )
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              )
            ],
          ),
        );
      }),
    );
  }
}

class _highlights extends ViewModelWidget<MembershipPageModel> {
  const _highlights({super.key});

  @override
  Widget build(BuildContext context, MembershipPageModel viewModel) {
    return RepaintBoundary(
      child: SizedBox(
        height: MediaQuery.of(context).size.width / 2.5,
        child: ListView(
          scrollDirection: Axis.horizontal,
          children: [
            if (viewModel.nextEvent != null)
              _highlightsCard(
                title: viewModel.nextEvent?.name ?? "",
                image: viewModel.nextEvent?.image ?? "",
                link: viewModel.nextEvent?.infoLink ?? "",
                onTap: viewModel.openExternalEvent(viewModel.nextEvent!),
              ),
            if (viewModel.lastSig != null)
              _highlightsCard(
                title: viewModel.lastSig?.name ?? "",
                image: viewModel.lastSig?.image ?? "",
                link: viewModel.lastSig?.link ?? "",
                onTap: viewModel.openExternalSig(viewModel.lastSig!),
              ),
            if (viewModel.lastBlogPost != null)
              _highlightsCard(
                title: viewModel.lastBlogPost?.title ?? "",
                image: viewModel.lastBlogPost?.enclosure?.url ?? "",
                link: viewModel.lastBlogPost?.link ?? "",
                onTap: viewModel.openExternalBlog(viewModel.lastBlogPost!),
              ),
          ],
        ),
      ),
    );
  }
}

class _highlightsCard extends StatelessWidget {
  final String title;
  final String image;
  final String link;
  final Function() onTap;

  const _highlightsCard({required this.title, required this.image, required this.link, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GestureDetector(
          onTap: onTap,
          child: Container(
            width: MediaQuery.of(context).size.width / 1.7,
            margin: EdgeInsets.all(MediaQuery.of(context).size.width / 80),
            decoration: BoxDecoration(
              color: kcPrimaryColor,
              borderRadius: BorderRadius.circular(20),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.2),
                  blurRadius: 3,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            clipBehavior: Clip.antiAlias,
            child: Column(
              children: [
                CachedNetworkImage(
                  imageUrl: image,
                  height: MediaQuery.of(context).size.width / 4,
                  width: double.infinity,
                  fit: BoxFit.cover,
                  maxHeightDiskCache: 294,
                  maxWidthDiskCache: 693,
                  memCacheHeight: 294,
                  memCacheWidth: 693,
                ),
                Padding(
                  padding: const EdgeInsets.all(8.0),
                  child: Text(
                    title,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const Spacer(),
      ],
    );
  }
}

class _addons extends ViewModelWidget<MembershipPageModel> {
  const _addons({super.key});

  @override
  Widget build(BuildContext context, MembershipPageModel viewModel) {
    return RepaintBoundary(
      child: Row(
        mainAxisAlignment: viewModel.favsAddons.length == 4 ? MainAxisAlignment.spaceEvenly : MainAxisAlignment.center,
        children: (viewModel.addons.map((addon) {
          return _addonsCard(
            onTap: viewModel.openExternalAddon(addon),
            name: addon.name,
            icon: Container(
              width: 40,
              height: 40,
              margin: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                image: DecorationImage(
                  image: CachedNetworkImageProvider(addon.icon),
                  fit: BoxFit.cover,
                  colorFilter: const ColorFilter.mode(
                    kcPrimaryColor,
                    BlendMode.srcATop,
                  ),
                ),
              ),
            ),
          );
        }).toList()
              ..addAll(internalAddonsList.where((element) => viewModel.hasInternalAddon(element)).map<_addonsCard>((e) {
                return _addonsCard(
                  onTap: viewModel.openInternalAddon(e),
                  name: e,
                  icon: Icon(
                    viewModel.getIconForInternalAddon(e),
                    color: kcPrimaryColor,
                    size: 40,
                  ),
                );
              }).toList()))
            .reversed
            .toList()
          ..sort((a, b) {
            return a.name.compareTo(b.name);
          }),
      ),
    );
  }
}

final AutoSizeGroup _addonTextGroup = AutoSizeGroup();

class _addonsCard extends StatelessWidget {
  final Widget icon;
  final Function() onTap;
  final String name;
  const _addonsCard({required this.icon, required this.onTap, required this.name});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: MediaQuery.of(context).size.width / 5.5 + MediaQuery.of(context).size.width / 80 * 2,
      child: Column(
        children: [
          GestureDetector(
            onTap: onTap,
            child: Center(
              child: Container(
                width: MediaQuery.of(context).size.width / 5.5,
                height: MediaQuery.of(context).size.width / 5.5,
                margin: EdgeInsets.all(MediaQuery.of(context).size.width / 80),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(15),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
                      blurRadius: 3,
                      offset: const Offset(0, 3),
                    ),
                  ],
                ),
                clipBehavior: Clip.antiAlias,
                child: icon,
              ),
            ),
          ),
          Padding(
            padding: EdgeInsets.symmetric(horizontal: MediaQuery.of(context).size.width / 80),
            child: AutoSizeText(
              "addons.${name.toLowerCase()}.title".tr(),
              style: const TextStyle(
                color: kcPrimaryColor,
                fontWeight: FontWeight.bold,
              ),
              minFontSize: 12,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              group: _addonTextGroup,
            ),
          ),
        ],
      ),
    );
  }
}
