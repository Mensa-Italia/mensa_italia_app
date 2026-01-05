import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_flip_card/controllers/flip_card_controllers.dart';
import 'package:flutter_flip_card/flipcard/flip_card.dart';
import 'package:flutter_flip_card/modal/flip_side.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/widgets/common/front_card_shine/front_card_shine.dart';
import 'package:stacked/stacked.dart';
import 'package:badges/badges.dart' as badges;
import 'package:html/parser.dart' as html_parser;
import 'membership_page_model.dart';

final internalAddonsList = ["Contacts", "TestMakers", "Documents", "Tableport", "Deals", "Boutique"];

String _decodeHtmlEntities(String? input) {
  if (input == null || input.isEmpty) {
    return "";
  }
  return html_parser.parseFragment(input).text ?? input;
}

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
        if (viewModel.nextEvent != null || viewModel.randomoSig != null || viewModel.lastBlogPost != null) ...[
          Padding(
            padding: EdgeInsets.all(8.0),
            child: Text("views.home.subtitle.highlights".tr()),
          ),
          const _highlights(
            key: ValueKey("Highlights"),
          ),
        ],
        if (viewModel.regSoci.isNotEmpty) ...[
          Padding(
            padding: EdgeInsets.all(8.0),
            child: Text("views.home.subtitle.birthdays".tr()),
          ),
          GestureDetector(
            onTap: viewModel.openBirthday,
            child: Container(
              height: 60,
              child: ListView(
                padding: const EdgeInsets.only(left: 20),
                physics: const NeverScrollableScrollPhysics(),
                scrollDirection: Axis.horizontal,
                children: [
                  for (var regSoci in viewModel.regSoci)
                    ConstrainedBox(
                      constraints: const BoxConstraints(
                        maxWidth: 40,
                        minWidth: 40,
                        maxHeight: 60,
                        minHeight: 60,
                      ),
                      child: OverflowBox(
                        maxWidth: 60,
                        minWidth: 60,
                        maxHeight: 60,
                        minHeight: 60,
                        child: Container(
                          width: 60,
                          height: 60,
                          decoration: BoxDecoration(
                            color: kcPrimaryColor.withOpacity(.4),
                            shape: BoxShape.circle,
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withOpacity(0.2),
                                blurRadius: 3,
                                offset: const Offset(0, 3),
                              ),
                            ],
                            image: DecorationImage(
                              fit: BoxFit.cover,
                              image: CachedNetworkImageProvider(
                                regSoci.image,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
          SizedBox(
            height: 20,
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
            badges.Badge(
              position: badges.BadgePosition.topEnd(top: -10, end: 0 - 5),
              badgeContent: Text(
                viewModel.unseenNotifications.toString(),
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onError,
                ),
              ),
              showBadge: viewModel.unseenNotifications > 0,
              badgeStyle: badges.BadgeStyle(
                badgeColor: Theme.of(context).colorScheme.error,
              ),
              child: IconButton(
                icon: const Icon(EneftyIcons.notification_outline),
                onPressed: viewModel.openNotifications,
                style: ButtonStyle(
                  backgroundColor: MaterialStateProperty.all(kcPrimaryColor.withOpacity(.1)),
                ),
              ),
            ),
            const SizedBox(
              width: 10,
            ),
            CircleAvatar(
              radius: 20,
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
            if (viewModel.randomoSig != null)
              _highlightsCard(
                title: viewModel.randomoSig?.name ?? "",
                image: viewModel.randomoSig?.image ?? "",
                link: viewModel.randomoSig?.link ?? "",
                onTap: viewModel.openExternalSig(viewModel.randomoSig!),
              ),
            if (viewModel.lastBlogPost != null)
              _highlightsCard(
                title: _decodeHtmlEntities(viewModel.lastBlogPost?.title),
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
              color: kcPrimaryColor.withOpacity(.4),
              borderRadius: BorderRadius.circular(20),
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
                      color: Colors.black,
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
    final caddonChildCardsData = addonChildCards(viewModel);
    return GridView.builder(
      padding: const EdgeInsets.all(8.0),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2, // Numero di colonne
        crossAxisSpacing: 10, // Spaziatura orizzontale
        mainAxisSpacing: 10, // Spaziatura verticale
        childAspectRatio: 3 / 1,
      ),
      physics: const NeverScrollableScrollPhysics(),
      shrinkWrap: true,
      itemCount: caddonChildCardsData.length,
      itemBuilder: (context, index) {
        return caddonChildCardsData[index];
      },
    );
  }

  List<Widget> addonChildCards(MembershipPageModel viewModel) {
    return viewModel.addons.map((addon) {
      return _addonsCard(
        onTap: viewModel.openExternalAddon(addon),
        name: addon.name,
        icon: Container(
          width: 35,
          height: 35,
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
          name: "addons.${e.toLowerCase()}.title".tr(),
          icon: Icon(
            viewModel.getIconForInternalAddon(e),
            color: kcPrimaryColor,
            size: 35,
          ),
        );
      }).toList())
      ..sort((a, b) {
        return a.name.compareTo(b.name);
      });
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
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(15),
          color: Colors.white,
        ),
        child: Row(
          children: [
            icon,
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(8.0),
                child: AutoSizeText(
                  name,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                  ),
                  minFontSize: 12,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  group: _addonTextGroup,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
