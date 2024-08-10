import 'package:flutter/cupertino.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:table_calendar/table_calendar.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventCalendarViewModel extends MasterModel {
  List<EventModel> events = [];
  DateTime selectedDate = DateTime.now();
  String selectedState = "Nearby & Online";
  Position? position;

  load() async {
    try {
      position ??= await determinePosition();
    } catch (_) {}
    Api().getEvents().then((value) {
      events.clear();
      events.addAll(value.where((element) {
        if (selectedState == "All" || element.owner == user.id) {
          return true;
        }
        if (element.position == null && selectedState.contains("Online")) {
          return true;
        }
        if (element.position != null &&
            selectedState.contains("Online") &&
            !selectedState.contains("Nearby")) {
          return false;
        }
        if (element.isNational) {
          return true;
        }
        if (!selectedState.contains("Nearby")) {
          return element.position?.state == selectedState;
        }
        if (position == null) {
          return false;
        } else {
          if (element.position == null) {
            return false;
          }
          final distance = const Distance().distance(
              LatLng(position!.latitude, position!.longitude),
              element.position!.toLatLong2());
          return distance < 90000;
        }
      }));
      rebuildUi();
    });
  }

  EventCalendarViewModel() {
    load();
  }

  bool isSelectedDay(DateTime day) {
    return isSameDay(selectedDate, day);
  }

  void onDaySelected(DateTime selectedDay, DateTime focusedDay) {
    selectedDate = selectedDay;
    rebuildUi();
  }

  List<EventModel> selectedDateEvents() {
    return events
        .where((element) =>
            isDateBetween(selectedDate, element.whenStart, element.whenEnd))
        .toList();
  }

  List retrieveEvents(DateTime day) {
    return events
        .where(
            (element) => isDateBetween(day, element.whenStart, element.whenEnd))
        .map((e) => e.name)
        .toList();
  }

  Function() onTapOnEvent(EventModel event) {
    return () async {
      if (event.infoLink.trim().isNotEmpty &&
          await canLaunchUrlString(event.infoLink.trim())) {
        launchUrlString(
          event.infoLink.trim(),
        );
      } else {
        dialogService.showDialog(
          title: 'Not ready yet',
          description: 'This event is being prepared, please try again later.',
        );
      }
    };
  }

  void changeSearchRadius() async {
    final UsableListOfStates = [
      "Nearby & Online",
      "Nearby",
      "Online",
      ...ListOfStates,
      "All"
    ];
    await showCupertinoModalPopup<void>(
      context: StackedService.navigatorKey!.currentContext!,
      builder: (BuildContext context) => Container(
        height: 216,
        padding: const EdgeInsets.only(top: 6.0),
        margin: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        color: CupertinoColors.systemBackground.resolveFrom(context),
        child: SafeArea(
          top: false,
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Spacer(),
                  CupertinoButton(
                    child: const Text(
                      'Done',
                      style: TextStyle(
                        color: kcPrimaryColor,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    onPressed: () {
                      Navigator.of(context).pop();
                    },
                  ),
                ],
              ),
              Expanded(
                child: CupertinoPicker(
                  itemExtent: 32.0,
                  scrollController: FixedExtentScrollController(
                    initialItem: UsableListOfStates.indexOf(selectedState),
                  ),
                  onSelectedItemChanged: (int index) {
                    selectedState = UsableListOfStates[index];
                    rebuildUi();
                  },
                  children: List<Widget>.generate(
                    UsableListOfStates.length,
                    (int index) {
                      return Center(
                        child: Text(
                          UsableListOfStates[index],
                        ),
                      );
                    },
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    load();
  }
}

bool isDateBetween(DateTime date, DateTime start, DateTime end) {
  return (date.isAfter(normalizeDate(start)) &&
          date.isBefore(normalizeDate(end))) ||
      isSameDay(date, start) ||
      isSameDay(date, end);
}
