import 'dart:math';

import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'events_map_viewmodel.dart';

class EventsMapView extends StackedView<EventsMapViewModel> {
  const EventsMapView({super.key});

  @override
  Widget builder(BuildContext context, EventsMapViewModel viewModel, Widget? child) {
    return Scaffold(
      extendBody: true,
      extendBodyBehindAppBar: true,
      appBar: getAppBarPlatform(
        previousPageTitle: "Events",
        title: "Events Map",
      ),
      body: MapLibreMap(
        styleString: "https://api.maptiler.com/maps/basic-v2/style.json?key=7u4KZex2hU8HDKij7YWx",
        initialCameraPosition: const CameraPosition(
          target: LatLng(42.715210940127285, 12.854392595268873),
          zoom: 4,
        ),
        compassEnabled: false,
        myLocationEnabled: true,
        myLocationRenderMode: MyLocationRenderMode.gps,
        onMapCreated: viewModel.onMapCreated,
        onUserLocationUpdated: viewModel.onUserLocationUpdated,
        onStyleLoadedCallback: viewModel.onStyleLoadedCallback,
        attributionButtonMargins: const Point<num>(-200, -200),
      ),
    );
  }

  @override
  EventsMapViewModel viewModelBuilder(BuildContext context) => EventsMapViewModel();
}
