import 'dart:math';
import 'dart:ui';

import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:stacked/stacked.dart';

import 'map_picker_viewmodel.dart';

class MapPickerView extends StackedView<MapPickerViewModel> {
  const MapPickerView({Key? key}) : super(key: key);

  @override
  Widget builder(
      BuildContext context, MapPickerViewModel viewModel, Widget? child) {
    return Scaffold(
      extendBody: true,
      appBar: const CupertinoNavigationBar(
        middle: Text('Select Location'),
        previousPageTitle: "Add Event",
      ),
      bottomNavigationBar: SafeArea(
        child: Container(
          margin: const EdgeInsets.all(20),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8)
              .copyWith(right: 8, left: 32),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(.9),
            borderRadius: const BorderRadius.all(Radius.circular(100)),
            boxShadow: const [
              BoxShadow(
                color: Colors.black12,
                blurRadius: 10,
                spreadRadius: 5,
              ),
            ],
          ),
          child: viewModel.locationName.isEmpty
              ? const Text("Zoom in to select a location",
                  textAlign: TextAlign.center,
                  style: TextStyle(
                      color: Colors.black, fontWeight: FontWeight.bold))
              : Row(
                  children: [
                    Expanded(
                      child: Text(
                        viewModel.locationName,
                        style: const TextStyle(
                          color: Colors.black,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                    IconButton(
                      onPressed: viewModel.onLocationSelected,
                      icon: const Icon(
                        EneftyIcons.tick_circle_outline,
                        size: 30,
                      ),
                    ),
                  ],
                ),
        ),
      ),
      body: SizedBox.expand(
        child: Stack(
          alignment: Alignment.center,
          children: [
            Positioned.fill(
              child: MapLibreMap(
                styleString:
                    "https://api.maptiler.com/maps/basic-v2/style.json?key=7u4KZex2hU8HDKij7YWx",
                initialCameraPosition: const CameraPosition(
                  target: LatLng(42.715210940127285, 12.854392595268873),
                  zoom: 4,
                ),
                trackCameraPosition: true,
                compassEnabled: false,
                myLocationEnabled: true,
                myLocationRenderMode: MyLocationRenderMode.gps,
                onMapCreated: viewModel.onMapCreated,
                onStyleLoadedCallback: viewModel.onStyleLoadedCallback,
                attributionButtonMargins: const Point<num>(-200, -200),
                onCameraIdle: viewModel.onCameraIdle,
              ),
            ),
            if (viewModel.locationName.isNotEmpty)
              Positioned(
                child: IgnorePointer(
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: 50),
                    child: Image.asset(
                      "assets/images/marker.png",
                      width: 50,
                      height: 50,
                      color: Colors.red,
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }

  @override
  MapPickerViewModel viewModelBuilder(BuildContext context) =>
      MapPickerViewModel();
}
