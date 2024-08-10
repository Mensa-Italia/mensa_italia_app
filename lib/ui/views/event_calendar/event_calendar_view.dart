import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';
import 'package:table_calendar/table_calendar.dart';

import 'event_calendar_viewmodel.dart';

class EventCalendarView extends StackedView<EventCalendarViewModel> {
  const EventCalendarView({Key? key}) : super(key: key);

  @override
  Widget builder(
      BuildContext context, EventCalendarViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: CupertinoNavigationBar(
        backgroundColor: Colors.white.withOpacity(0.6),
        previousPageTitle: "Events",
        middle: const Text('Calendar'),
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TableCalendar(
            firstDay: DateTime.now().subtract(const Duration(days: 365 * 5)),
            lastDay: DateTime.now().add(const Duration(days: 365 * 5)),
            focusedDay: viewModel.selectedDate,
            selectedDayPredicate: viewModel.isSelectedDay,
            eventLoader: viewModel.retrieveEvents,
            onDaySelected: viewModel.onDaySelected,
            availableCalendarFormats: const {
              CalendarFormat.month: 'Month',
            },
            calendarBuilders: CalendarBuilders(
              selectedBuilder: (context, date, events) => Container(
                margin: const EdgeInsets.all(4.0),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: kcPrimaryColor,
                  borderRadius: BorderRadius.circular(8.0),
                ),
                child: Text(
                  date.day.toString(),
                  style: const TextStyle(color: Colors.white),
                ),
              ),
              headerTitleBuilder: (context, title) {
                return Padding(
                  padding: const EdgeInsets.only(top: 8, bottom: 8),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          DateFormat.yMMMM().format(title),
                        ),
                      ),
                      ElevatedButton(
                        onPressed: viewModel.changeSearchRadius,
                        iconAlignment: IconAlignment.end,
                        style: ButtonStyle(
                          visualDensity: VisualDensity.compact,
                          backgroundColor:
                              const WidgetStatePropertyAll(Colors.white),
                          padding: const WidgetStatePropertyAll(
                              EdgeInsets.symmetric(horizontal: 10)),
                          side: const WidgetStatePropertyAll(
                              BorderSide(color: Colors.black)),
                          shape: WidgetStatePropertyAll(RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(20))),
                        ),
                        child: Text(
                          viewModel.selectedState,
                          style: const TextStyle(
                            color: Colors.black,
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ],
                  ),
                );
              },
              dowBuilder: (context, day) {
                if (day.weekday == DateTime.sunday) {
                  final text = DateFormat.E().format(day);
                  return Center(
                    child: Text(
                      text,
                      style: const TextStyle(color: Colors.red),
                    ),
                  );
                }
                return null;
              },
            ),
          ),
          Expanded(
            child: ListView.builder(
              itemCount: viewModel.selectedDateEvents().length,
              itemBuilder: (context, index) {
                final event = viewModel.selectedDateEvents()[index];
                return _EventTile(event: event);
              },
            ),
          ),
        ],
      ),
    );
  }

  @override
  EventCalendarViewModel viewModelBuilder(BuildContext context) =>
      EventCalendarViewModel();
}

class _EventTile extends ViewModelWidget<EventCalendarViewModel> {
  final EventModel event;

  const _EventTile({required this.event});

  @override
  Widget build(BuildContext context, EventCalendarViewModel viewModel) {
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
                                style: const TextStyle(
                                    fontWeight: FontWeight.normal,
                                    fontSize: 12),
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
