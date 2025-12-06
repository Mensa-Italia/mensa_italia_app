import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'tickets_viewmodel.dart';

class TicketsView extends StackedView<TicketsViewModel> {
  final String previousPageTitle;
  const TicketsView({Key? key, required this.previousPageTitle}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    TicketsViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          getAppBarSliverPlatform(
            title: viewModel.componentName.tr(),
            previousPageTitle: previousPageTitle.tr(),
          ),
          if (viewModel.tickets.isEmpty && !viewModel.isBusy)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      "views.tickets.no_tickets".tr(),
                    ),
                  ],
                ),
              ),
            ),
          if (viewModel.isBusy)
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(),
              ),
            ),
          if (viewModel.tickets.isNotEmpty && !viewModel.isBusy)
            SliverList.builder(
              itemBuilder: (context, index) {
                final ticket = viewModel.tickets[index];
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
                  title: Text(ticket.name ?? ""),
                  subtitle: Text(ticket.description ?? ""),
                  leading: AspectRatio(
                    aspectRatio: 1,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Theme.of(context).primaryColor.withOpacity(.1),
                        shape: BoxShape.circle,
                      ),
                      alignment: Alignment.center,
                      child: Icon(
                        EneftyIcons.ticket_2_outline,
                        color: Theme.of(context).primaryColor,
                      ),
                    ),
                  ),
                  onTap: viewModel.tapOnTicket(index),
                );
              },
              itemCount: viewModel.tickets.length,
            ),
          SliverSafeArea(sliver: SliverToBoxAdapter(child: SizedBox(height: 100))),
        ],
      ),
    );
  }

  @override
  TicketsViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      TicketsViewModel();
}
