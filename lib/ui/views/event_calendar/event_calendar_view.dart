import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/widgets/common/event_tile/event_tile.dart';
import 'package:stacked/stacked.dart';
import 'package:table_calendar/table_calendar.dart';

import 'event_calendar_viewmodel.dart';

class EventCalendarView extends StackedView<EventCalendarViewModel> {
  const EventCalendarView({super.key});

  @override
  Widget builder(
      BuildContext context, EventCalendarViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        previousPageTitle: "Events",
        title: "Events Calendar",
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
            startingDayOfWeek: StartingDayOfWeek.monday,
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
                return EventTile(
                  event: event,
                  onTap: viewModel.onTapOnEvent(event),
                );
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
