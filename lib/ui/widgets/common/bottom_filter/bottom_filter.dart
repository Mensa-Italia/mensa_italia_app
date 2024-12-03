import 'package:expandable_widgets/expandable_widgets.dart';
import 'package:flutter/material.dart';
import 'package:modal_bottom_sheet/modal_bottom_sheet.dart';
import 'package:stacked/stacked.dart';

import 'bottom_filter_model.dart';

class BottomFilter extends StackedView<BottomFilterModel> {
  const BottomFilter({super.key});

  @override
  Widget builder(
    BuildContext context,
    BottomFilterModel viewModel,
    Widget? child,
  ) {
    return Material(
      child: SingleChildScrollView(
        controller: ModalScrollController.of(context),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Filters',
              style: TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: const Text(
                    'Type',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: viewModel.pickType,
                  child: Text(viewModel.selectedType),
                ),
              ],
            ),
            Row(
              children: [
                Expanded(
                  child: const Text(
                    'State',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                TextButton(
                  onPressed: viewModel.pickState,
                  child: Text(viewModel.selectedState),
                ),
              ],
            ),
            if (viewModel.selectedState.toLowerCase().contains("nearby"))
              Row(
                children: [
                  Expanded(
                    child: const Text(
                      'Nearby distance',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  Slider(
                    value: viewModel.distance,
                    onChanged: viewModel.pickDistance,
                    min: 10,
                    max: 100,
                    divisions: 100,
                    label: "${viewModel.distance.toInt()} km",
                  ),
                ],
              ),
            const SafeArea(child: SizedBox(height: 0)),
          ],
        ),
      ),
    );
  }

  @override
  BottomFilterModel viewModelBuilder(
    BuildContext context,
  ) =>
      BottomFilterModel();
}
