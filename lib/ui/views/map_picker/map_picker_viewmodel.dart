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
      (partialLocation.northeast.latitude +
              partialLocation.southwest.latitude) /
          2,
      (partialLocation.northeast.longitude +
              partialLocation.southwest.longitude) /
          2,
    );
    Placemark place = placemarks[0];
    locationToUse = place;
    locationCoordinates = LatLng(
      (partialLocation.northeast.latitude +
              partialLocation.southwest.latitude) /
          2,
      (partialLocation.northeast.longitude +
              partialLocation.southwest.longitude) /
          2,
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
