import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/views/add_event_schedule_list/add_event_schedule_list_view.dart';
import 'package:mensa_italia_app/ui/widgets/common/map_shower/map_shower.dart';
import 'package:stacked/stacked.dart';

import 'event_showcase_viewmodel.dart';

class EventShowcaseView extends StackedView<EventShowcaseViewModel> {
  final EventModel event;
  const EventShowcaseView({Key? key, required this.event}) : super(key: key);

  @override
  Widget builder(BuildContext context, EventShowcaseViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: event.name,
        previousPageTitle: 'Events',
      ),
      extendBody: true,
      extendBodyBehindAppBar: true,
      bottomNavigationBar: GestureDetector(
        onTap: viewModel.openUrl,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 10).copyWith(top: 20),
          color: kcPrimaryColor,
          child: const SafeArea(
            top: false,
            child: Text(
              "DETAILS",
              style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
              textAlign: TextAlign.center,
            ),
          ),
        ),
      ),
      body: ListView(
        children: [
          Container(
            margin: const EdgeInsets.only(top: 20),
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Hero(
              tag: event.image,
              transitionOnUserGestures: true,
              flightShuttleBuilder: (flightContext, animation, flightDirection, fromHeroContext, toHeroContext) => AnimatedBuilder(
                animation: animation,
                builder: (context, child) {
                  return ClipRRect(
                    borderRadius: BorderRadius.only(
                      bottomLeft: Radius.circular(30 * (animation.value)),
                      bottomRight: Radius.circular(30 * (animation.value)),
                      topLeft: Radius.circular((30 * (animation.value)) + (10 * (1 - animation.value))),
                      topRight: Radius.circular((30 * (animation.value)) + (10 * (1 - animation.value))),
                    ),
                    child: child,
                  );
                },
                child: CachedNetworkImage(
                  imageUrl: event.image,
                  fit: BoxFit.cover,
                  memCacheHeight: 554,
                  memCacheWidth: 1059,
                  maxHeightDiskCache: 554,
                  maxWidthDiskCache: 1059,
                ),
              ),
              child: Container(
                decoration: BoxDecoration(
                  borderRadius: const BorderRadius.all(Radius.circular(30)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
                      blurRadius: 5,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                clipBehavior: Clip.antiAlias,
                child: CachedNetworkImage(
                  width: double.infinity,
                  imageUrl: event.image,
                  memCacheHeight: 554,
                  memCacheWidth: 1059,
                  maxHeightDiskCache: 554,
                  maxWidthDiskCache: 1059,
                ),
              ),
            ),
          ),
          const SizedBox(height: 20),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Text(
              event.description,
              style: const TextStyle(
                fontSize: 16,
              ),
            ),
          ),
          const SizedBox(
            height: 20,
          ),
          if (viewModel.eventSchedules.isNotEmpty) ...[
            const Text("Schedule", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
            const Divider(),
            ListView.separated(
                padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 20),
                itemCount: viewModel.eventSchedules.length,
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemBuilder: (context, index) {
                  final event = viewModel.eventSchedules[index];
                  return TileSchedue(eventSchedule: event, onTap: () {});
                },
                separatorBuilder: (context, index) {
                  if (index != viewModel.eventSchedules.length - 1 && !DateUtils.isSameDay(viewModel.eventSchedules[index].whenStart, viewModel.eventSchedules[index + 1].whenStart)) {
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const SizedBox(height: 30),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 0),
                          child: Text(
                            DateFormat('EEEE, d MMMM').format(viewModel.eventSchedules[index + 1].whenStart),
                            style: TextStyle(
                              color: Colors.grey.shade500,
                              height: 0,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const Divider(),
                      ],
                    );
                  }
                  return const Divider();
                }),
            const Divider(),
            const SizedBox(height: 20),
          ],
          const Text("Location", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
          const SizedBox(
            height: 20,
          ),
          GestureDetector(
            onTap: viewModel.openMap,
            child: Container(
              margin: const EdgeInsets.symmetric(horizontal: 20),
              height: 200,
              clipBehavior: Clip.antiAlias,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.2),
                    blurRadius: 5,
                    spreadRadius: 2,
                  ),
                ],
              ),
              child: MapShower(
                pointPosition: event.position!.toLatLng(),
              ),
            ),
          ),
          const SafeArea(child: SizedBox(height: 100)),
        ],
      ),
    );
  }

  @override
  EventShowcaseViewModel viewModelBuilder(BuildContext context) => EventShowcaseViewModel(event: event);
}
