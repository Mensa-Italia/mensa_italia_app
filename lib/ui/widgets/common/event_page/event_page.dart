import 'package:cached_network_image/cached_network_image.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
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
            return _EventTile(event: viewModel.events[index]);
          },
        ),
        const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  EventPageModel viewModelBuilder(BuildContext context) => EventPageModel();
}

class _EventTile extends ViewModelWidget<EventPageModel> {
  final EventModel event;

  const _EventTile({required this.event});

  @override
  Widget build(BuildContext context, EventPageModel viewModel) {
    if (event.isNational) {
      return GestureDetector(
        onTap: viewModel.onTapOnEvent(event),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 10),
          child: Container(
            decoration: BoxDecoration(
              color: kcPrimaryColor,
              borderRadius: BorderRadius.circular(10),
            ),
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                AspectRatio(
                  child: CachedNetworkImage(
                    imageUrl: event.image,
                    fit: BoxFit.cover,
                  ),
                  aspectRatio: 16 / 9,
                ),
                Container(
                  padding: const EdgeInsets.all(10),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          event.name,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Text(
                        DateFormat.yMMMd().format(event.whenStart),
                        style: const TextStyle(
                          fontSize: 14,
                          color: Colors.white,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    } else {
      return GestureDetector(
        onTap: viewModel.onTapOnEvent(event),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 10),
          child: Container(
            height: 100,
            decoration: BoxDecoration(
              color: kcPrimaryColor.withOpacity(.2),
              borderRadius: BorderRadius.circular(10),
            ),
            clipBehavior: Clip.antiAlias,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: CachedNetworkImage(
                    imageUrl: event.image,
                    fit: BoxFit.cover,
                  ),
                ),
                Container(
                  padding: const EdgeInsets.all(5).copyWith(left: 10),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text.rich(
                          TextSpan(
                            text: event.name,
                            children: [
                              const TextSpan(text: '\n'),
                              TextSpan(
                                text: event.position?.state ?? "Online",
                                style: const TextStyle(fontWeight: FontWeight.normal, fontSize: 12),
                              ),
                            ],
                          ),
                          style: const TextStyle(
                            fontSize: 16,
                            height: 1.1,
                            fontWeight: FontWeight.w500,
                            color: Colors.black,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Text(
                        DateFormat.yMMMd().format(event.whenStart),
                        style: const TextStyle(
                          fontSize: 14,
                          color: Colors.black,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }
  }
}
