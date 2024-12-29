import 'package:flutter/foundation.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class FilterNotification extends ChangeNotifier {
  static final FilterNotification _instance = FilterNotification._();
  factory FilterNotification() => _instance;

  FilterNotification._();
  void notify() {
    notifyListeners();
  }
}

class BottomFilterModel extends MasterModel {
  final usableListOfStates = [
    "Nearby & Online",
    "Nearby",
    "Online",
    "All",
    ...ListOfStates
  ];
  final eventTypes = ["All", "International", "National", "Local", "Spot"];

  String selectedState = "Nearby & Online";
  String selectedType = "All";
  double distance = 20;

  BottomFilterModel() {
    Api().getMetadata().then((value) {
      selectedState = value["eventfilter_state"] ?? "Nearby & Online";
      selectedType = value["eventfilter_type"] ?? "All";
      distance = double.parse((value["eventfilter_distance"] ?? "20"));
      rebuildUi();
    });
  }

  void pickState() {
    cupertinoModalPicker(
      title: "States",
      initialItem: usableListOfStates.indexOf(selectedState),
      items: usableListOfStates,
    ).then((value) {
      if (value != null) {
        selectedState = value;
        Api().setMetadata("eventfilter_state", selectedState).then((value) {
          FilterNotification().notify();
        });

        rebuildUi();
      }
    });
  }

  void pickType() {
    cupertinoModalPicker(
      title: "Types",
      initialItem: eventTypes.indexOf(selectedType),
      items: eventTypes,
    ).then((value) {
      if (value != null) {
        selectedType = value;
        Api().setMetadata("eventfilter_type", selectedType).then((value) {
          FilterNotification().notify();
        });
        rebuildUi();
      }
    });
  }

  void pickDistance(double value) {
    distance = value;
    Api()
        .setMetadata("eventfilter_distance", distance.toString())
        .then((value) {
      FilterNotification().notify();
    });
    rebuildUi();
  }
}
