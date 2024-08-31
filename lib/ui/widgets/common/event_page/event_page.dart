import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/event_tile/event_tile.dart';
import 'package:stacked/stacked.dart';

import 'event_page_model.dart';

class EventPage extends StackedView<EventPageModel> {
  const EventPage({super.key});

  @override
  Widget builder(BuildContext context, EventPageModel viewModel, Widget? child) {
    return getCustomScrollViewPlatform(
      slivers: [
        getAppBarSliverPlatform(
          title: 'Events',
          leading: (viewModel.allowControlEvents())
              ? CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.navigateToAddEvent,
                  child: const Icon(
                    EneftyIcons.add_circle_bold,
                    color: kcPrimaryColor,
                  ),
                )
              : null,
          trailings: [
            CupertinoButton(
              padding: EdgeInsets.zero,
              onPressed: viewModel.navigateToCalendar,
              child: const Icon(
                EneftyIcons.calendar_2_outline,
                color: kcPrimaryColor,
              ),
            ),
            CupertinoButton(
              padding: EdgeInsets.zero,
              onPressed: viewModel.navigateToMap,
              child: const Icon(
                EneftyIcons.map_2_outline,
                color: kcPrimaryColor,
              ),
            ),
          ],
          searchBarActions: SearchBarActions(
            onChanged: viewModel.search,
            controller: viewModel.searchController,
            onSubmitted: viewModel.search,
          ),
          trailingTitle: [
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
                  viewModel.selectedState,
                  style: const TextStyle(
                    color: Colors.black,
                    fontSize: 12,
                  ),
                ),
                icon: const Icon(
                  EneftyIcons.location_outline,
                  color: Colors.black,
                ),
              ),
            ),
          ],
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
              key: ValueKey(event.id),
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
