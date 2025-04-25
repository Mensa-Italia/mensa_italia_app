import 'package:flutter/material.dart';
import 'package:form_validator/form_validator.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'addon_deals_add_viewmodel.dart';

class AddonDealsAddView extends StackedView<AddonDealsAddViewModel> {
  final DealModel? deal;
  const AddonDealsAddView({super.key, this.deal});

  @override
  Widget builder(
      BuildContext context, AddonDealsAddViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(title: "Add Deal", previousPageTitle: "Deals"),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            Form(
              key: viewModel.formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  TextFormField(
                    decoration: const InputDecoration(hintText: 'Deal Name'),
                    controller: viewModel.dealNameController,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Please enter the deal name';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration:
                        const InputDecoration(hintText: 'Commercial Sector'),
                    controller: viewModel.commercialSectorController,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Please enter the commercial sector';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration:
                        const InputDecoration(hintText: 'Position (Optional)'),
                    controller: viewModel.locationController,
                    canRequestFocus: false,
                    enableInteractiveSelection: false,
                    onTap: viewModel.pickLocation,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: viewModel.dateTimeEvent,
                    onTap: viewModel.pickDateTime,
                    canRequestFocus: false,
                    enableInteractiveSelection: false,
                    decoration: const InputDecoration(
                      hintText: 'When',
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration:
                        const InputDecoration(hintText: 'Details (Optional)'),
                    controller: viewModel.detailsController,
                    maxLines: 4,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration: const InputDecoration(
                        hintText: 'Who is Eligible (Optional)'),
                    controller: viewModel.whoController,
                    canRequestFocus: false,
                    enableInteractiveSelection: false,
                    onTap: viewModel.selectEligibility,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration: const InputDecoration(
                        hintText: 'How to Get (Optional)'),
                    controller: viewModel.howToGetController,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration:
                        const InputDecoration(hintText: 'Link (Optional)'),
                    controller: viewModel.linkController,
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    decoration: const InputDecoration(
                        hintText: 'VAT Number (Optional)'),
                    controller: viewModel.vatNumberController,
                  ),
                  const SizedBox(height: 24),
                  const Divider(),
                  const SizedBox(height: 24),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.red,
                      borderRadius: BorderRadius.circular(25),
                    ),
                    child: Column(
                      children: [
                        const Text.rich(
                          TextSpan(
                            text: 'Contact Informations\n',
                            style: TextStyle(
                              fontSize: 16,
                              color: Colors.white,
                            ),
                            children: [
                              TextSpan(
                                text: '(Hidden from public)',
                                style: TextStyle(
                                  fontSize: 14,
                                  color: Colors.white,
                                  fontWeight: FontWeight.normal,
                                ),
                              ),
                            ],
                          ),
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          decoration: const InputDecoration(hintText: "Name"),
                          controller: viewModel.contactName,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          decoration: const InputDecoration(
                              hintText: "Email (Optional)"),
                          controller: viewModel.contactEmail,
                          validator: ValidationBuilder().email(
                            "Please enter a valid email address",
                          ).build(),
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          decoration: const InputDecoration(
                              hintText: "Phone (Optional)"),
                          controller: viewModel.contactPhone,
                        ),
                        const SizedBox(height: 16),
                        TextFormField(
                          decoration: const InputDecoration(
                              hintText: 'Notes (Optional)'),
                          controller: viewModel.contactNotes,
                          maxLines: 4,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  const Divider(),
                  const SizedBox(height: 24),
                  ElevatedButton(
                    onPressed: viewModel.submitDeal,
                    child: viewModel.isBusy
                        ? LoadingAnimationWidget.beat(
                            color: Colors.white.withOpacity(.8),
                            size: 20,
                          )
                        : const Text('Submit'),
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
  AddonDealsAddViewModel viewModelBuilder(BuildContext context) =>
      AddonDealsAddViewModel(deal: deal);
}
