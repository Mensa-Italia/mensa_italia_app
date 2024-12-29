import 'package:enefty_icons/enefty_icons.dart';
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
        trailings: [
          IconButton(
            onPressed: viewModel.changeSearchRadius,
            icon: Icon(
              EneftyIcons.filter_outline,
              color: Theme.of(context).appBarTheme.iconTheme?.color,
            ),
            iconSize: Theme.of(context).appBarTheme.iconTheme?.size,
          ),
        ],
      ),
      body: Column(
        mainAxisAlignment: MainAxisAlignment.start,
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
              markerBuilder: (context, date, events) {
                if (events.isNotEmpty) {
                  List<EventModel> eventsToUse =
                      viewModel.retrieveDateEvents(date);
                  List<Color> colors = [];
                  List<double> stops = [];
                  for (var event in eventsToUse) {
                    if (!colors.contains(getColorForDot(event))) {
                      colors.add(getColorForDot(event));
                    }
                  }
                  for (var i = 0; i < colors.length; i++) {
                    stops.add(i / colors.length);
                  }
                  if (colors.length > 1) {
                    return Container(
                      width: (colors.length > 3 ? 3 : colors.length) * 7,
                      height: 7,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: colors,
                          begin: Alignment.centerLeft,
                          end: Alignment.centerRight,
                          stops: stops,
                        ),
                        borderRadius: BorderRadius.circular(8.0),
                      ),
                    );
                  }
                  return Container(
                    width:
                        (eventsToUse.length > 3 ? 3 : eventsToUse.length) * 7,
                    height: 7,
                    decoration: BoxDecoration(
                      color: colors.first,
                      borderRadius: BorderRadius.circular(8.0),
                    ),
                  );
                }
                return null;
              },
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
                          textAlign: TextAlign.center,
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

  Color getColorForDot(EventModel event) {
    if (event.isSpot) {
      return Color(0xFF874dff);
    }
    if (event.position?.state == "NaN") {
      return Color(0xFFeca41e);
    }
    if (event.isNational) {
      return kcPrimaryColor;
    }
    return const Color.fromARGB(255, 138, 169, 230);
  }

  @override
  EventCalendarViewModel viewModelBuilder(BuildContext context) =>
      EventCalendarViewModel();
}
