import 'dart:io';
import 'dart:math';
import 'dart:ui';

import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:maplibre_gl/maplibre_gl.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:place_picker_google/place_picker_google.dart';
import 'package:stacked/stacked.dart';

import 'map_picker_viewmodel.dart';

class MapPickerView extends StackedView<MapPickerViewModel> {
  const MapPickerView({super.key});

  @override
  Widget builder(
      BuildContext context, MapPickerViewModel viewModel, Widget? child) {
    return Scaffold(
      extendBody: true,
      appBar: getAppBarPlatform(
        title: "viewModel.locationMapPicker.title".tr(),
        previousPageTitle: "Locations",
      ),
      resizeToAvoidBottomInset: true,
      bottomNavigationBar: true
          ? null
          : SafeArea(
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
      body: true
          ? SizedBox.expand(
              child: PlacePicker(
                apiKey: Platform.isAndroid
                    ? 'AIzaSyCxFp5eTX30garvxqET8CEIeHuzI93gqfo'
                    : 'AIzaSyB2aoF70O1oDQv0BQ3SG6WFQaayIEVEMPE',
                onPlacePicked: viewModel.resultLocation,
                searchInputConfig: const SearchInputConfig(
                  padding: EdgeInsets.symmetric(
                    horizontal: 16.0,
                    vertical: 8.0,
                  ),
                  autofocus: false,
                ),
                searchInputDecorationConfig: SearchInputDecorationConfig(
                  hintText: "viewModel.locationMapPicker.searchLocation".tr(),
                ),
                localizationConfig: LocalizationConfig.init(
                  languageCode: Platform.localeName,
                  selectActionLocation:
                      "viewModel.locationMapPicker.selectActionLocation".tr(),
                  unnamedLocation:
                      "viewModel.locationMapPicker.unnamedLocation".tr(),
                  noResultsFound:
                      "viewModel.locationMapPicker.noResultsFound".tr(),
                  findingPlace: "viewModel.locationMapPicker.findingPlace".tr(),
                  nearBy: "viewModel.locationMapPicker.nearBy".tr(),
                ),
                myLocationButtonEnabled: true,
                enableNearbyPlaces: false,
              ),
            )
          : SizedBox.expand(
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
                  if (viewModel.searchFocusNode.hasFocus)
                    Positioned(
                      top: 10,
                      left: 10,
                      right: 10,
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: BackdropFilter(
                          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
                          child: Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: Colors.red.withOpacity(.3),
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(color: Colors.red, width: 1),
                            ),
                            child: const Text(
                              "The search data may not be accurate, please verify the location and move the marker to the correct location",
                              style: TextStyle(
                                color: Colors.black,
                                fontWeight: FontWeight.bold,
                              ),
                              textAlign: TextAlign.center,
                            ),
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
