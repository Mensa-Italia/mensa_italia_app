import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_linkify/flutter_linkify.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/views/add_event_schedule_list/add_event_schedule_list_view.dart';
import 'package:mensa_italia_app/ui/views/event_showcase/phone_linkifier.dart';
import 'package:mensa_italia_app/ui/widgets/common/map_shower/map_shower.dart';
import 'package:stacked/stacked.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'event_showcase_viewmodel.dart';

class EventShowcaseView extends StackedView<EventShowcaseViewModel> {
  final String previousPageTitle;
  final EventModel event;
  const EventShowcaseView({super.key, required this.previousPageTitle, required this.event});

  @override
  Widget builder(
      BuildContext context, EventShowcaseViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: event.name,
        previousPageTitle: previousPageTitle,
        trailings: [
          IconButton(
            icon: Icon(
              Theme.of(context).platform == TargetPlatform.iOS
                  ? CupertinoIcons.share_up
                  : Icons.share,
              color: Theme.of(context).appBarTheme.iconTheme?.color,
            ),
            onPressed: viewModel.shareEvent,
            iconSize: Theme.of(context).appBarTheme.iconTheme?.size,
          ),
          if ((event.owner == viewModel.user.id) || viewModel.isSuper())
            IconButton(
              icon: Icon(EneftyIcons.edit_outline,
                  color: Theme.of(context).appBarTheme.iconTheme?.color),
              onPressed: viewModel.editEvent,
              iconSize: Theme.of(context).appBarTheme.iconTheme?.size,
            ),
        ],
      ),
      extendBody: true,
      extendBodyBehindAppBar: true,
      bottomNavigationBar: event.infoLink.isNotEmpty
          ? GestureDetector(
              onTap: viewModel.openUrl,
              child: Container(
                padding:
                    const EdgeInsets.symmetric(vertical: 10).copyWith(top: 20),
                color: kcPrimaryColor,
                child: SafeArea(
                  top: false,
                  child: Text(
                    "views.eventdetails.button.details".tr().toUpperCase(),
                    style: TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
            )
          : null,
      body: ListView(
        children: [
          if (event.image.isNotEmpty)
            Hero(
              tag: event.image,
              transitionOnUserGestures: true,
              flightShuttleBuilder: (flightContext, animation, flightDirection,
                      fromHeroContext, toHeroContext) =>
                  AnimatedBuilder(
                animation: animation,
                builder: (context, child) {
                  return ClipRRect(
                    borderRadius: BorderRadius.only(),
                    child: child,
                  );
                },
                child: CachedNetworkImage(
                  imageUrl: event.image,
                  fit: BoxFit.cover,
                ),
              ),
              child: Container(
                decoration: BoxDecoration(),
                clipBehavior: Clip.antiAlias,
                child: CachedNetworkImage(
                  width: double.infinity,
                  imageUrl: event.image,
                ),
              ),
            ),
          if (event.isSpot)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20)
                  .copyWith(top: 10, bottom: 0),
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.circular(10),
                ),
                clipBehavior: Clip.antiAlias,
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.all(8.0).copyWith(left: 20),
                      child: Icon(
                        EneftyIcons.warning_2_outline,
                        size: 35,
                        color: Colors.white,
                      ),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(10.0).copyWith(right: 20),
                        child: Text(
                          "views.eventdetails.warning.spotevent".tr(),
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 15),
            child: Text.rich(
              TextSpan(
                text: "views.eventdetails.details.title".tr(),
                children: [],
              ),
              style: TextStyle(
                  fontSize: 30, fontWeight: FontWeight.bold, height: 1.1),
              textAlign: TextAlign.start,
            ),
          ),
          SizedBox(
            height: 10,
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Linkify(
              text: event.description,
              linkifiers: [
                UrlLinkifier(),
                EmailLinkifier(),
                PhoneLinkifier(),
              ],
              onOpen: (link) {
                launchUrlString(link.url);
              },
              linkStyle: const TextStyle(
                color: kcPrimaryColor,
                decoration: TextDecoration.underline,
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
              style: const TextStyle(
                fontSize: 16,
              ),
            ),
          ),
          const SizedBox(
            height: 40,
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 15),
            child: Text.rich(
              TextSpan(
                text: "views.eventdetails.schedule.title".tr(),
              ),
              style: TextStyle(
                  fontSize: 30, fontWeight: FontWeight.bold, height: 1.1),
              textAlign: TextAlign.start,
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(20.0).copyWith(bottom: 0, top: 10),
            child: DateTimeStartEndBoxes(
              start: event.whenStart,
              end: event.whenEnd,
            ),
          ),
          if (viewModel.eventSchedules.isNotEmpty) ...[
            const Divider(),
            ListView.separated(
                padding:
                    const EdgeInsets.symmetric(vertical: 10, horizontal: 20),
                itemCount: viewModel.eventSchedules.length,
                physics: const NeverScrollableScrollPhysics(),
                shrinkWrap: true,
                itemBuilder: (context, index) {
                  final event = viewModel.eventSchedules[index];
                  return TileSchedue(eventSchedule: event, onTap: () {});
                },
                separatorBuilder: (context, index) {
                  if (index != viewModel.eventSchedules.length - 1 &&
                      !DateUtils.isSameDay(
                          viewModel.eventSchedules[index].whenStart,
                          viewModel.eventSchedules[index + 1].whenStart)) {
                    return Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        const SizedBox(height: 30),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 0),
                          child: Text(
                            DateFormat('EEEE, d MMMM').format(
                                viewModel.eventSchedules[index + 1].whenStart),
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
          ],
          const SizedBox(height: 20),
          if (event.position != null) ...[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 15),
              child: Text.rich(
                TextSpan(
                  text: "views.eventdetails.location.title".tr(),
                  children: [
                    TextSpan(
                      text: "\n${event.position!.getAddress()}",
                      style: TextStyle(
                          color: kcPrimaryColor,
                          fontSize: 14,
                          fontWeight: FontWeight.normal),
                    ),
                  ],
                ),
                style: TextStyle(
                    fontSize: 30, fontWeight: FontWeight.bold, height: 1.1),
                textAlign: TextAlign.start,
              ),
            ),
            const SizedBox(
              height: 20,
            ),
            GestureDetector(
              onTap: viewModel.openMap,
              child: Container(
                height: 200 + MediaQuery.of(context).padding.bottom,
                clipBehavior: Clip.antiAlias,
                decoration: BoxDecoration(),
                child: MapShower(
                  pointPosition: event.position!.toLatLng(),
                ),
              ),
            ),
            const SizedBox(height: 20),
          ],
          if (viewModel.owners != null) ...[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 15),
              child: Text.rich(
                TextSpan(
                  text: "views.eventdetails.organizers.title".tr(),
                ),
                style: TextStyle(
                    fontSize: 30, fontWeight: FontWeight.bold, height: 1.1),
                textAlign: TextAlign.start,
              ),
            ),
            SizedBox(
              height: 20,
            ),
            ...viewModel.owners!.map((owner) {
              return InkWell(
                onTap: () {
                  launchUrlString("mailto:${owner.email}");
                },
                child: Row(
                  children: [
                    const SizedBox(width: 20),
                    CircleAvatar(
                      radius: 30,
                      backgroundImage: CachedNetworkImageProvider(owner.avatar),
                    ),
                    const SizedBox(width: 20),
                    Expanded(
                      child: Text.rich(
                        TextSpan(
                          text: owner.name,
                          children: [
                            TextSpan(
                              text: "\n${owner.email}",
                              style: TextStyle(
                                color: kcPrimaryColor,
                                fontSize: 14,
                                fontWeight: FontWeight.normal,
                              ),
                            ),
                          ],
                        ),
                        style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            height: 1.1),
                        textAlign: TextAlign.start,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ],
          const SizedBox(height: 100),
        ],
      ),
    );
  }

  @override
  EventShowcaseViewModel viewModelBuilder(BuildContext context) =>
      EventShowcaseViewModel(event: event);
}

class DateTimeStartEndBoxes extends StatelessWidget {
  final DateTime start;
  final DateTime end;
  const DateTimeStartEndBoxes(
      {super.key, required this.start, required this.end});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Text.rich(
                TextSpan(
                  children: [
                    TextSpan(
                      text: "views.eventdetails.schedule.from".tr(),
                      style: TextStyle(fontSize: 12),
                    ),
                    TextSpan(
                      text: "\n",
                    ),
                    TextSpan(
                      text: DateFormat('HH:mm').format(start),
                      style: TextStyle(
                          color: kcPrimaryColor,
                          fontSize: 16,
                          fontWeight: FontWeight.bold),
                    ),
                    TextSpan(
                      text: "\n",
                    ),
                    TextSpan(
                      text: DateFormat('d MMMM').format(start),
                      style: TextStyle(fontSize: 12),
                    ),
                  ],
                ),
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ),
        Icon(EneftyIcons.arrow_right_4_outline),
        Expanded(
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Text.rich(
                TextSpan(
                  children: [
                    TextSpan(
                      text: "views.eventdetails.schedule.to".tr(),
                      style: TextStyle(fontSize: 12),
                    ),
                    TextSpan(
                      text: "\n",
                    ),
                    TextSpan(
                      text: DateFormat('HH:mm').format(end),
                      style: TextStyle(
                          color: kcPrimaryColor,
                          fontSize: 16,
                          fontWeight: FontWeight.bold),
                    ),
                    TextSpan(
                      text: "\n",
                    ),
                    TextSpan(
                      text: DateFormat('d MMMM').format(end),
                      style: TextStyle(fontSize: 12),
                    ),
                  ],
                ),
                textAlign: TextAlign.center,
              ),
            ),
          ),
        ),
      ],
    );
  }
}
