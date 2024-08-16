import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'addon_deals_add_viewmodel.dart';

class AddonDealsAddView extends StackedView<AddonDealsAddViewModel> {
  const AddonDealsAddView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, AddonDealsAddViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(title: "Add Deal", previousPageTitle: "Deals"),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            Form(
              key: viewModel.formKey, // Add a GlobalKey for validation
              child: Column(
                children: [
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Deal Name'),
                    controller: viewModel.dealNameController,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Please enter the deal name';
                      }
                      return null;
                    },
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Commercial Sector'),
                    controller: viewModel.commercialSectorController,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Please enter the commercial sector';
                      }
                      return null;
                    },
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Position (Optional)'),
                    controller: viewModel.positionController,
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Starting Date (YYYY-MM-DD)'),
                    controller: viewModel.startingDateController,
                    keyboardType: TextInputType.datetime,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Please enter the starting date';
                      }
                      return null;
                    },
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Ending Date (YYYY-MM-DD)'),
                    controller: viewModel.endingDateController,
                    keyboardType: TextInputType.datetime,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Please enter the ending date';
                      }
                      return null;
                    },
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Details (Optional)'),
                    controller: viewModel.detailsController,
                    maxLines: 4,
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Who is Eligible (Optional)'),
                    controller: viewModel.whoController,
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'How to Get (Optional)'),
                    controller: viewModel.howToGetController,
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'Link (Optional)'),
                    controller: viewModel.linkController,
                  ),
                  SizedBox(height: 16),
                  TextFormField(
                    decoration: InputDecoration(labelText: 'VAT Number (Optional)'),
                    controller: viewModel.vatNumberController,
                  ),
                  SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: () {
                      if (viewModel.formKey.currentState!.validate()) {
                        // Handle form submission
                        viewModel.submitDeal();
                      }
                    },
                    child: Text('Submit'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  AddonDealsAddViewModel viewModelBuilder(BuildContext context) => AddonDealsAddViewModel();
}