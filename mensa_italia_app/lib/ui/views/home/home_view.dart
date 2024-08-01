import 'dart:ui';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter_flip_card/flutter_flip_card.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/views/sigs_list/sigs_list_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/addon_page/addon_page.dart';
import 'package:mensa_italia_app/ui/widgets/common/event_page/event_page.dart';
import 'package:mensa_italia_app/ui/widgets/common/front_card_shine/front_card_shine.dart';
import 'package:mensa_italia_app/ui/widgets/common/option_page/option_page.dart';
import 'package:stacked/stacked.dart';
import 'home_viewmodel.dart';

class HomeView extends StackedView<HomeViewModel> {
  const HomeView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, HomeViewModel viewModel, Widget? child) {
    precacheImage(const AssetImage("assets/images/backcard.jpg"), context);
    return Scaffold(
      extendBody: true,
      bottomNavigationBar: ClipRect(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
          child: Theme(
            data: Theme.of(context).copyWith(
              splashColor: Colors.transparent,
            ),
            child: Container(
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(
                    color: Colors.grey.withOpacity(0.5),
                    width: 0.5,
                  ),
                ),
              ),
              child: BottomNavigationBar(
                elevation: 0,
                type: BottomNavigationBarType.fixed,
                backgroundColor: Colors.white.withOpacity(0.8),
                showSelectedLabels: false,
                showUnselectedLabels: false,
                enableFeedback: false,
                items: const [
                  BottomNavigationBarItem(
                    icon: Icon(EneftyIcons.ticket_outline),
                    label: "Events",
                    activeIcon: Icon(EneftyIcons.ticket_bold),
                    backgroundColor: Colors.transparent,
                  ),
                  BottomNavigationBarItem(
                    icon: AutoSizeText(
                      "SiGs",
                      style: TextStyle(
                          fontWeight: FontWeight.w900,
                          fontSize: 20,
                          color: kcMediumGrey),
                      minFontSize: 0,
                    ),
                    label: "SiGs",
                    activeIcon: AutoSizeText(
                      "SiGs",
                      style: TextStyle(
                          fontWeight: FontWeight.w900,
                          fontSize: 20,
                          color: kcPrimaryColor),
                      minFontSize: 0,
                    ),
                    backgroundColor: Colors.transparent,
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(EneftyIcons.home_outline),
                    label: "Home",
                    activeIcon: Icon(EneftyIcons.home_bold),
                    backgroundColor: Colors.transparent,
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(EneftyIcons.category_outline),
                    label: "Addons",
                    activeIcon: Icon(EneftyIcons.category_bold),
                    backgroundColor: Colors.transparent,
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(EneftyIcons.more_square_outline),
                    label: "Options",
                    activeIcon: Icon(EneftyIcons.more_square_bold),
                    backgroundColor: Colors.transparent,
                  ),
                ],
                currentIndex: viewModel.currentIndex,
                onTap: viewModel.bottomBarTapped,
              ),
            ),
          ),
        ),
      ),
      body: switchBetweenPages(viewModel),
    );
  }

  Widget switchBetweenPages(HomeViewModel viewModel) {
    switch (viewModel.currentIndex) {
      case 0:
        return const EventPage();
      case 1:
        return const SigsListView();
      case 2:
        return home();
      case 3:
        return const AddonPage();
      case 4:
        return const OptionPage();
      default:
        return const Center(child: Text("Home"));
    }
  }

  Widget home() {
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
        const _UserInfoTopBar(),
        _MembershipCard(),
      ],
    );
  }

  @override
  HomeViewModel viewModelBuilder(BuildContext context) => HomeViewModel();
}

class _UserInfoTopBar extends ViewModelWidget<HomeViewModel> {
  const _UserInfoTopBar({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, HomeViewModel viewModel) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 30),
      child: Row(
        children: [
          Text.rich(
            TextSpan(
              text: 'Hello!\n',
              style: const TextStyle(
                fontSize: 16,
                height: 1,
              ),
              children: [
                TextSpan(
                  text: viewModel.user.name.split(" ").first,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 18,
                  ),
                ),
              ],
            ),
          ),
          const Expanded(child: SizedBox()),
          CircleAvatar(
              radius: 25,
              backgroundImage:
                  CachedNetworkImageProvider(viewModel.user.avatar)),
        ],
      ),
    );
  }
}

class _MembershipCard extends ViewModelWidget<HomeViewModel> {
  @override
  Widget build(BuildContext context, HomeViewModel viewModel) {
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

  Widget front(HomeViewModel viewModel) {
    return const FrontCardShine();
  }

  Widget back(HomeViewModel viewModel) {
    var nomeProfilo = viewModel.user.name.replaceFirst(" ", "~~~").split("~~~");
    var ntessera = viewModel.user.id;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: kcPrimaryColor,
        borderRadius: BorderRadius.circular(20),
        image: DecorationImage(
          image: const AssetImage("assets/images/backcard.jpg"),
          fit: BoxFit.cover,
          colorFilter: ColorFilter.mode(
              Colors.white.withOpacity(0.3), BlendMode.srcATop),
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
      child: LayoutBuilder(
          builder: (BuildContext context, BoxConstraints constraints) {
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
                              const Column(
                                  mainAxisAlignment: MainAxisAlignment.start,
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: []);
                            }
                            return Column(
                              mainAxisAlignment: MainAxisAlignment.start,
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: List.generate(nomeProfilo.length, (i) {
                                return Expanded(
                                  child: AutoSizeText(
                                      nomeProfilo[i].toUpperCase().trim(),
                                      style: const TextStyle(
                                          color: Colors.black,
                                          fontWeight: FontWeight.bold,
                                          fontSize: 20),
                                      textAlign: TextAlign.left,
                                      minFontSize: 0,
                                      maxLines: 1),
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
                                width: constraints.maxWidth -
                                    (constraints.maxWidth * 2 / 7),
                                alignment: Alignment.bottomLeft,
                                child: Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  mainAxisSize: MainAxisSize.max,
                                  crossAxisAlignment: CrossAxisAlignment.end,
                                  children: <Widget>[
                                    AutoSizeText(ntessera,
                                        style: const TextStyle(
                                            color: Colors.black,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 20),
                                        minFontSize: 0),
                                    const AutoSizeText(
                                      "MENSA.IT",
                                      style: TextStyle(
                                          fontWeight: FontWeight.bold,
                                          fontSize: 14),
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
