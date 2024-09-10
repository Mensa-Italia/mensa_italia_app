import 'dart:math';

import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:mensa_italia_app/ui/views/events_map/events_map_viewmodel.dart';
import 'package:stacked/stacked.dart';

import 'map_shower_model.dart';

class MapShower extends StackedView<MapShowerModel> {
  final LatLng pointPosition;
  const MapShower({super.key, required this.pointPosition});

  @override
  Widget builder(
      BuildContext context, MapShowerModel viewModel, Widget? child) {
    return AbsorbPointer(
      child: MapLibreMap(
        styleString:
            "https://api.maptiler.com/maps/basic-v2/style.json?key=7u4KZex2hU8HDKij7YWx",
        initialCameraPosition: CameraPosition(
          target: pointPosition,
          zoom: 17,
        ),
        tiltGesturesEnabled: false,
        rotateGesturesEnabled: false,
        zoomGesturesEnabled: false,
        scrollGesturesEnabled: false,
        compassEnabled: false,
        myLocationEnabled: false,
        onMapCreated: (controller) {
          loadMarkerImage('assets/images/marker_blue.png').then((value) async {
            await controller.addImage('marker_cs_image_blue', value);
            controller.addSymbol(SymbolOptions(
              geometry: pointPosition,
              iconImage: 'marker_cs_image_blue',
              iconSize: .4,
              iconAnchor: "bottom",
            ));
          });
        },
        attributionButtonMargins: const Point<num>(-200, -200),
      ),
    );
  }

  @override
  MapShowerModel viewModelBuilder(BuildContext context) => MapShowerModel();
}
