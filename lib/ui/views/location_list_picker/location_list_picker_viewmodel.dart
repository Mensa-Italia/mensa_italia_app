import 'dart:io';

import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_viewmodel.dart';
import 'package:place_picker_google/place_picker_google.dart';
import 'package:stacked/stacked.dart';

class LocationListPickerViewModel extends MasterModel {
  List<LocationModel> locations = [];
  void addLocation() {
    navigationService.navigateToMapPickerView().then((value) {
      if (value != null && value is LocationSelected) {
        Api()
            .createLocation(
          name: value.locationName,
          address: value.locationAddress,
          latitude: value.coordinates.latitude,
          longitude: value.coordinates.longitude,
        )
            .then((location) {
          locations.add(location);
          rebuildUi();
        });
      }
    });
  }

  LocationListPickerViewModel() {
    init();
  }

  init() {
    setBusy(true);
    Api().getLocaitons().then((value) {
      locations.clear();
      locations.addAll(value);
      setBusy(false);
    });
  }

  Function() deleteLocation(String id) {
    return () {
      Api().deleteLocation(id).then((value) {
        locations.removeWhere((element) => element.id == id);
        rebuildUi();
      });
    };
  }
}
