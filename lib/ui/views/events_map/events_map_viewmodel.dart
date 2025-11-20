import 'dart:math';

import 'package:flutter/services.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class EventsMapViewModel extends MasterModel {
  MapLibreMapController? mapController;

  List<EventModel> events = [];

  load() {
    Api().getEvents().then((value) {
      events = value.where((element) => element.position != null).toList();
      advancedMapUsage();
    });
  }

  void onUserLocationUpdated(UserLocation location) {}

  void onMapCreated(MapLibreMapController controller) {
    mapController = controller;
    controller.onFeatureTapped.add(onSymbolTapped);
  }

  void onSymbolTapped(
    Point<double> point,
    LatLng coordinates,
    String id,
    String layerId,
    Annotation? annotation,
  ) async {
    final event = events.firstWhere((element) => element.id == id);
    navigationService.navigateToEventShowcaseView(event: event);
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
    Future.delayed(const Duration(seconds: 1), () {
      load();
    });
  }

  void advancedMapUsage() async {
    await loadMarkerImage('assets/images/marker.png').then((value) async {
      await mapController!.addImage('marker_cs_image', value);
    });

    await loadMarkerImage('assets/images/marker_blue.png').then((value) async {
      await mapController!.addImage('marker_cs_image_blue', value);
    });
    await loadMarkerImage('assets/images/marker_blue_spot.png')
        .then((value) async {
      await mapController!.addImage('marker_cs_image_blue_spot', value);
    });

    await mapController?.addSource(
      "events-source",
      GeojsonSourceProperties(
        data: {
          "type": "FeatureCollection",
          "features": events
              .where((e) => e.position != null)
              .map(
                (e) => {
                  "type": "Feature",
                  "id": e.id,
                  "geometry": {
                    "type": "Point",
                    "coordinates": [e.position!.lon, e.position!.lat],
                  },
                  "properties": {
                    "icon-image": e.isNational
                        ? "marker_cs_image"
                        : e.isSpot
                            ? "marker_cs_image_blue_spot"
                            : "marker_cs_image_blue",
                    "icon-size": e.isNational
                        ? 0.35
                        : e.isSpot
                            ? 0.20
                            : 0.30,
                    "title": e.isNational ? e.name : "",
                    "event": e.toJson(),
                  },
                },
              )
              .toList(),
        },
      ),
    );
    await mapController!.addSymbolLayer(
      "events-source",
      "events-layer",
      const SymbolLayerProperties(
        iconImage: "{icon-image}",
        iconSize: ['get', 'icon-size'],
        iconAllowOverlap: true,
        iconIgnorePlacement: true,
        textAllowOverlap: true,
        textIgnorePlacement: true,
        iconAnchor: "bottom",
        iconPitchAlignment: "viewport",
        textField: "{title}",
        textAnchor: "top",
        textColor: "#000000",
        textHaloColor: "#ffffff",
        textHaloWidth: 1,
        textSize: [
          "interpolate",
          ["linear"],
          ["zoom"],
          8,
          0,
          12,
          18
        ],
      ),
      enableInteraction: true,
    );
  }
}

Future<Uint8List> loadMarkerImage(String image) async {
  try {
    var byteData = await rootBundle.load(image);
    return byteData.buffer.asUint8List();
  } catch (e) {
    return Uint8List(0);
  }
}
