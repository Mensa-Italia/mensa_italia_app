import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/widgets/common/event_tile/event_tile.dart';
import 'package:stacked/stacked.dart';

import 'event_page_model.dart';

class EventPage extends StackedView<EventPageModel> {
  const EventPage({super.key});

  @override
  Widget builder(BuildContext context, EventPageModel viewModel, Widget? child) {
    return CustomScrollView(
      physics: const AlwaysScrollableScrollPhysics(),
      controller: viewModel.scrollController,
      anchor: 0.06,
      slivers: [
        CupertinoSliverNavigationBar(
          largeTitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  const Text(
                    'Events',
                    style: TextStyle(fontWeight: FontWeight.w900),
                  ),
                  const Expanded(child: SizedBox()),
                  Padding(
                    padding: const EdgeInsets.only(right: 15, top: 3),
                    child: TextButton.icon(
                      onPressed: viewModel.changeSearchRadius,
                      style: TextButton.styleFrom(
                        visualDensity: VisualDensity.compact,
                        padding: const EdgeInsets.only(left: 10, right: 5),
                      ),
                      iconAlignment: IconAlignment.end,
                      label: Text(
                        viewModel.selectedState.contains("Nearby") ? "${viewModel.selectedState} (90km)" : viewModel.selectedState,
                        style: const TextStyle(
                          color: Colors.black,
                          fontSize: 12,
                        ),
                      ),
                      icon: const Icon(
                        EneftyIcons.location_bold,
                        color: Colors.black,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(
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
          backgroundColor: Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
          border: null,
          middle: const Text(
            'Events',
            style: TextStyle(fontWeight: FontWeight.bold),
          ),
          alwaysShowMiddle: false,
          leading: (viewModel.allowControlEvents())
              ? CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.navigateToAddEvent,
                  child: const Icon(
                    CupertinoIcons.add_circled_solid,
                    color: kcPrimaryColor,
                  ),
                )
              : null,
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: viewModel.navigateToCalendar,
                child: const Icon(
                  CupertinoIcons.calendar,
                  color: kcPrimaryColor,
                ),
              ),
              CupertinoButton(
                padding: EdgeInsets.zero,
                onPressed: viewModel.navigateToMap,
                child: const Icon(
                  CupertinoIcons.map,
                  color: kcPrimaryColor,
                ),
              )
            ],
          ),
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        if (viewModel.events.isEmpty)
          const SliverFillRemaining(
            child: SafeArea(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                mainAxisAlignment: MainAxisAlignment.start,
                children: [
                  Icon(
                    EneftyIcons.ticket_2_outline,
                    size: 50,
                    color: Colors.grey,
                  ),
                  SizedBox(height: 10),
                  Text(
                    'No events found',
                    style: TextStyle(
                      color: Colors.grey,
                      fontSize: 20,
                    ),
                  ),
                ],
              ),
            ),
          ),
        SliverList.builder(
          itemCount: viewModel.events.length,
          itemBuilder: (context, index) {
            final event = viewModel.events[index];
            return EventTile(
              event: event,
              onTap: viewModel.onTapOnEvent(event),
              onLongTap: (viewModel.allowControlEvents() && event.owner == viewModel.user.id) || viewModel.isSuper() ? viewModel.onLongTapEditEvent(event) : null,
            );
          },
        ),
        const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  EventPageModel viewModelBuilder(BuildContext context) => EventPageModel();
}
