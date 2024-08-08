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
  Widget builder(BuildContext context, EventCalendarViewModel viewModel, Widget? child) {
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
  EventCalendarViewModel viewModelBuilder(BuildContext context) => EventCalendarViewModel();
}

class _EventTile extends ViewModelWidget<EventCalendarViewModel> {
  final EventModel event;

  const _EventTile({required this.event});

  @override
  Widget build(BuildContext context, EventCalendarViewModel viewModel) {
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
              CachedNetworkImage(imageUrl: event.image, fit: BoxFit.cover),
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
                      DateFormat.yMMMd().format(event.when),
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
  }
}
