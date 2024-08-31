import 'package:dio/dio.dart';
import 'package:flutter/cupertino.dart';
import 'package:geocoding/geocoding.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:mensa_italia_app/app/app.dialogs.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class LocationSelected {
  final String locationName;
  final Placemark? location;
  final LatLng coordinates;

  LocationSelected({
    required this.locationName,
    this.location,
    required this.coordinates,
  });
}

class MapPickerViewModel extends MasterModel {
  MapLibreMapController? mapController;
  Placemark? locationToUse;
  String get locationName => locationToUse?.name ?? "";
  LatLng locationCoordinates = LatLng(0, 0);
  String isSearching = "";

  TextEditingController searchController = TextEditingController();

  FocusNode searchFocusNode = FocusNode();

  onSearchChanged(String p1) {
    if (p1.isEmpty || p1.length < 5) {
      isSearching = "";
      rebuildUi();
      return;
    }
    onSearchSubmitted(p1);
  }

  onSearchSubmitted(String p1) {
    Dio().get("https://photon.komoot.io/api/?q=${Uri.encodeQueryComponent(p1)}&lang=de").then((value) {
      mapController
          ?.moveCamera(
        CameraUpdate.newLatLngZoom(
          LatLng(value.data["features"][0]["geometry"]["coordinates"][1], value.data["features"][0]["geometry"]["coordinates"][0]),
          15,
        ),
      )
          .then((value) {
        onCameraIdle();
      });
    });
  }

  void onMapCreated(MapLibreMapController controller) {
    mapController = controller;
  }

  void onStyleLoadedCallback() async {
    mapController?.requestMyLocationLatLng().then((value) async {
      if (value != null) {
        mapController?.moveCamera(
          CameraUpdate.newLatLngZoom(
            LatLng(value.latitude, value.longitude),
            8,
          ),
        );
      }
    });
  }

  void onCameraIdle() async {
    if (mapController?.cameraPosition == null) return;
    if (mapController!.cameraPosition!.zoom < 13) {
      locationToUse = null;
      rebuildUi();
      return;
    }
    LatLngBounds partialLocation = (await mapController!.getVisibleRegion());
    List<Placemark> placemarks = await placemarkFromCoordinates(
      (partialLocation.northeast.latitude + partialLocation.southwest.latitude) / 2,
      (partialLocation.northeast.longitude + partialLocation.southwest.longitude) / 2,
    );
    Placemark place = placemarks[0];
    locationToUse = place;
    locationCoordinates = LatLng(
      (partialLocation.northeast.latitude + partialLocation.southwest.latitude) / 2,
      (partialLocation.northeast.longitude + partialLocation.southwest.longitude) / 2,
    );

    rebuildUi();
  }

  void onLocationSelected() {
    if (locationToUse != null) {
      dialogService
          .showCustomDialog<String, String>(
        variant: DialogType.inputText,
        title: "Location selected",
        description: "If the address is not correct, please correct it",
        data: locationToUse!.name,
      )
          .then((value) {
        if (value != null && value.data != null) {
          navigationService.back(
            result: LocationSelected(
              locationName: value.data!,
              location: locationToUse!,
              coordinates: locationCoordinates,
            ),
          );
          rebuildUi();
        }
      });
    }
  }
}
