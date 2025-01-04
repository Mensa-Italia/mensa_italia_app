import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'event_tile_model.dart';

class EventTile extends StackedView<EventTileModel> {
  final EventModel event;
  final Function() onTap;
  final Function? onLongTap;
  const EventTile(
      {super.key, required this.event, required this.onTap, this.onLongTap});

  @override
  Widget builder(
      BuildContext context, EventTileModel viewModel, Widget? child) {
    return GestureDetector(
      onTap: onTap,
      onLongPress: () {
        if (onLongTap == null) return;
        viewModel.setBusy(false);
        onLongTap!();
      },
      onTapDown: (details) {
        if (onLongTap == null) return;
        viewModel.setBusy(true);
      },
      onTapUp: (details) {
        if (onLongTap == null) return;
        viewModel.setBusy(false);
      },
      onLongPressEnd: (details) {
        if (onLongTap == null) return;
        viewModel.setBusy(false);
      },
      child: AnimatedScale(
        scale: viewModel.isBusy ? 0.95 : 1,
        duration: const Duration(milliseconds: 300),
        child: getEventTile(),
      ),
    );
  }

  Widget getEventTile() {
    if (event.isNational) {
      return Padding(
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
                aspectRatio: 16 / 9,
                child: Hero(
                  transitionOnUserGestures: true,
                  tag: event.image,
                  child: ClipRRect(
                    borderRadius: const BorderRadius.only(
                      topLeft: Radius.circular(10),
                      topRight: Radius.circular(10),
                    ),
                    child: CachedNetworkImage(
                      width: double.infinity,
                      imageUrl: event.image,
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
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
                        fontSize: 12,
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
      );
    } else if (event.isSpot) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 10),
        child: Container(
          decoration: BoxDecoration(
            color: Color(0xFF874dff).withOpacity(.4),
            borderRadius: BorderRadius.circular(10),
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                padding: const EdgeInsets.all(5).copyWith(left: 10, right: 10),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text.rich(
                            TextSpan(
                              text: event.name,
                            ),
                            style: const TextStyle(
                              fontSize: 16,
                              height: 1.1,
                              fontWeight: FontWeight.w500,
                              color: Colors.black,
                            ),
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                          Text.rich(
                            TextSpan(
                              text: event.position?.state != null
                                  ? (event.position?.state == "NaN"
                                      ? "International"
                                      : event.position?.state)
                                  : "Online",
                            ),
                            style: const TextStyle(
                              fontWeight: FontWeight.normal,
                              fontSize: 12,
                              height: 1.1,
                              color: Colors.black,
                            ),
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                        ],
                      ),
                    ),
                    SizedBox(
                      width: 20,
                    ),
                    Text(
                      DateFormat.yMMMd().format(event.whenStart),
                      style: const TextStyle(
                        fontSize: 12,
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
      );
    } else {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 10),
        child: Container(
          decoration: BoxDecoration(
            color: event.position?.state == "NaN"
                ? Color(0xFFeca41e)
                : kcPrimaryColor.withOpacity(.2),
            borderRadius: BorderRadius.circular(10),
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              AspectRatio(
                aspectRatio: 16 / 4,
                child: Hero(
                  transitionOnUserGestures: true,
                  tag: event.image,
                  child: CachedNetworkImage(
                    imageUrl: event.image,
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              Container(
                padding: const EdgeInsets.all(5).copyWith(left: 10, right: 10),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text.rich(
                            TextSpan(
                              text: event.name,
                            ),
                            style: const TextStyle(
                              fontSize: 16,
                              height: 1.1,
                              fontWeight: FontWeight.w500,
                              color: Colors.black,
                            ),
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                          Text.rich(
                            TextSpan(
                              text: event.position?.state != null
                                  ? (event.position?.state == "NaN"
                                      ? "International"
                                      : event.position?.state)
                                  : "Online",
                            ),
                            style: const TextStyle(
                              fontWeight: FontWeight.normal,
                              fontSize: 12,
                              height: 1.1,
                              color: Colors.black,
                            ),
                            overflow: TextOverflow.ellipsis,
                            maxLines: 1,
                          ),
                        ],
                      ),
                    ),
                    SizedBox(
                      width: 20,
                    ),
                    Text(
                      DateFormat.yMMMd().format(event.whenStart),
                      style: const TextStyle(
                        fontSize: 12,
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
      );
    }
  }

  @override
  EventTileModel viewModelBuilder(BuildContext context) => EventTileModel();
}
