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
import 'package:mensa_italia_app/ui/widgets/common/membership_page/membership_page.dart';
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
                      style: TextStyle(fontWeight: FontWeight.w900, fontSize: 20, color: kcMediumGrey),
                      minFontSize: 0,
                    ),
                    label: "SiGs",
                    activeIcon: AutoSizeText(
                      "SiGs",
                      style: TextStyle(fontWeight: FontWeight.w900, fontSize: 20, color: kcPrimaryColor),
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
        return const MembershipPage();
      case 3:
        return const AddonPage();
      case 4:
        return const OptionPage();
      default:
        return const Center(child: Text("Home"));
    }
  }

  @override
  HomeViewModel viewModelBuilder(BuildContext context) => HomeViewModel();
}
