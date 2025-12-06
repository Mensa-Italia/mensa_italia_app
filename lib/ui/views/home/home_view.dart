import 'dart:ui';

import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/services/tickets_see%20.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/widgets/common/sigs_page/sigs_page_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/addon_page/addon_page.dart';
import 'package:mensa_italia_app/ui/widgets/common/event_page/event_page.dart';
import 'package:mensa_italia_app/ui/widgets/common/membership_page/membership_page.dart';
import 'package:mensa_italia_app/ui/widgets/common/option_page/option_page.dart';
import 'package:pulsing_border/pulsing_border.dart';
import 'package:stacked/stacked.dart';
import 'home_viewmodel.dart';

class HomeView extends StackedView<HomeViewModel> {
  const HomeView({super.key});

  @override
  Widget builder(BuildContext context, HomeViewModel viewModel, Widget? child) {
    precacheImage(const AssetImage("assets/images/backcard.jpg"), context);

    return Scaffold(
      extendBody: true,
      floatingActionButton: (TicketSSE().tickets.isNotEmpty)
          ? PulsingBorder(
              color: Colors.purple,
              borderRadius: 300,
              spreadRadius: 20,
              blurRadius: 0,
              pulseDuration: const Duration(milliseconds: 1000),
              pulseDelay: const Duration(milliseconds: 200),
              child: FloatingActionButton(
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(300),
                ),
                onPressed: () {
                  if (viewModel.currentIndex == 0) {
                    viewModel.navigationService.navigateToTicketsView(
                      previousPageTitle: "views.events.title",
                    );
                  }
                  if (viewModel.currentIndex == 1) {
                    viewModel.navigationService.navigateToTicketsView(
                      previousPageTitle: "views.community.title",
                    );
                  }
                  if (viewModel.currentIndex == 2) {
                    viewModel.navigationService.navigateToTicketsView(
                      previousPageTitle: "views.home.title",
                    );
                  }
                  if (viewModel.currentIndex == 3) {
                    viewModel.navigationService.navigateToTicketsView(
                      previousPageTitle: "views.addons.title",
                    );
                  }
                  if (viewModel.currentIndex == 4) {
                    viewModel.navigationService.navigateToTicketsView(
                      previousPageTitle: "views.settings.title",
                    );
                  }
                },
                backgroundColor: Colors.purple,
                foregroundColor: Colors.white,
                child: const Icon(EneftyIcons.ticket_2_outline),
              ),
            )
          : null,
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
                    icon: Icon(EneftyIcons.people_outline),
                    label: "SiGs",
                    activeIcon: Icon(EneftyIcons.people_bold),
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
        return const SigsPage();
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
