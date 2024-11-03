import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
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
          title: "views.events.title".tr(),
          leading: (viewModel.allowControlEvents())
              ? IconButton(
                  padding: EdgeInsets.zero,
                  onPressed: viewModel.navigateToAddEvent,
                  icon: Icon(
                    EneftyIcons.add_circle_bold,
                    color: Theme.of(context).appBarTheme.iconTheme?.color,
                  ),
                  iconSize: Theme.of(context).appBarTheme.iconTheme?.size,
                )
              : null,
          trailings: [
            IconButton(
              padding: EdgeInsets.zero,
              onPressed: viewModel.navigateToCalendar,
              icon: Icon(
                EneftyIcons.calendar_2_outline,
                color: Theme.of(context).appBarTheme.iconTheme?.color,
              ),
              iconSize: Theme.of(context).appBarTheme.iconTheme?.size,
            ),
            IconButton(
              padding: EdgeInsets.zero,
              onPressed: viewModel.navigateToMap,
              icon: Icon(
                EneftyIcons.map_2_outline,
                color: Theme.of(context).appBarTheme.iconTheme?.color,
              ),
              iconSize: Theme.of(context).appBarTheme.iconTheme?.size,
            ),
          ],
          searchBarActions: SearchBarActions(
            onChanged: viewModel.search,
            controller: viewModel.searchController,
            onSubmitted: viewModel.search,
            hintText: "views.events.search.textfield.hint".tr(),
          ),
          trailingTitle: [
            IconButton(
              onPressed: viewModel.changeSearchRadius,
              icon: Icon(
                EneftyIcons.filter_outline,
                color: Theme.of(context).appBarTheme.iconTheme?.color,
              ),
            ),
          ],
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        if (viewModel.events.isEmpty)
          SliverFillRemaining(
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
                    "views.events.empty".tr(),
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
