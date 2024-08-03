import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'map_picker_viewmodel.dart';

class MapPickerView extends StackedView<MapPickerViewModel> {
  const MapPickerView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    MapPickerViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      backgroundColor: Theme.of(context).colorScheme.background,
      body: Container(
        padding: const EdgeInsets.only(left: 25.0, right: 25.0),
      ),
    );
  }

  @override
  MapPickerViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      MapPickerViewModel();
}
