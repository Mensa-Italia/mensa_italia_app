import 'package:flutter/cupertino.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:url_launcher/url_launcher_string.dart';

class EventPageModel extends MasterModel {
  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  final List<EventModel> _originalEvents = [];
  final List<EventModel> events = [];
  String selectedState = "Nearby & Online";
  Position? position;

  EventPageModel() {
    load();
  }

  load() async {
    try {
      position ??= await determinePosition();
    } catch (_) {}
    Api().getEvents().then((value) {
      _originalEvents.clear();
      _originalEvents.addAll(value);
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
    events.sort(
      (a, b) {
        if (a.isNational && !b.isNational) {
          return -1;
        }
        if (!a.isNational && b.isNational) {
          return 1;
        }
        return a.whenStart.compareTo(b.whenStart);
      },
    );
  }

  void search(String value) {
    if (value.isEmpty) {
      events.clear();
      events.addAll(_originalEvents);
      rebuildUi();
      return;
    }
    events.clear();
    events.addAll(_originalEvents.where((element) {
      return element.name.toLowerCase().contains(value.toLowerCase());
    }));
    rebuildUi();
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

  void navigateToMap() {
    navigationService.navigateToEventsMapView();
  }

  void navigateToAddEvent() {
    navigationService.navigateToAddEventView().then((value) {
      load();
    });
  }

  void navigateToCalendar() {
    navigationService.navigateToEventCalendarView();
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

  Function() onLongTapEditEvent(EventModel event) {
    return () async {
      navigationService.navigateToAddEventView(event: event).then((value) {
        load();
      });
    };
  }
}
