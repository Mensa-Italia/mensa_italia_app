import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'location_list_picker_viewmodel.dart';

class LocationListPickerView extends StackedView<LocationListPickerViewModel> {
  final String previousPageTitle;
  const LocationListPickerView({Key? key, required this.previousPageTitle}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    LocationListPickerViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      extendBodyBehindAppBar: true,
      appBar: getAppBarPlatform(
        previousPageTitle: previousPageTitle.tr(),
        title:  viewModel.componentName.tr()
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: viewModel.addLocation,
        backgroundColor: kcPrimaryColor,
        label: const Text(
          "Add location",
          style: TextStyle(
            color: Colors.white,
          ),
        ),
        icon: const Icon(
          EneftyIcons.add_outline,
          color: Colors.white,
        ),
      ),
      body: viewModel.isBusy
          ? const Center(
              child: CircularProgressIndicator(),
            )
          : viewModel.locations.isEmpty
              ? const Center(
                  child: Text(
                    "No locations found",
                    style: TextStyle(
                      fontSize: 20,
                      color: Colors.black,
                    ),
                  ),
                )
              : ListView.builder(
                  itemCount: viewModel.locations.length,
                  itemBuilder: (context, index) {
                    final location = viewModel.locations[index];
                    return ListTile(
                      visualDensity: VisualDensity.compact,
                      leading: Container(
                        width: 50,
                        height: 50,
                        decoration: BoxDecoration(
                          color: kcPrimaryColor.withOpacity(.5),
                          borderRadius: BorderRadius.circular(300),
                        ),
                        child: Icon(
                          EneftyIcons.location_bold,
                          color: Colors.white,
                          size: 30,
                        ),
                      ),
                      trailing: IconButton(
                        icon: const Icon(
                          EneftyIcons.trash_outline,
                          color: Colors.red,
                        ),
                        onPressed: viewModel.deleteLocation(location.id),
                      ),
                      title: Text(location.name),
                      subtitle: Text(location.getAddress()),
                      onTap: () {
                        viewModel.navigationService.back(
                          result: location,
                        );
                      },
                    );
                  },
                ),
    );
  }

  @override
  LocationListPickerViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      LocationListPickerViewModel();
}
