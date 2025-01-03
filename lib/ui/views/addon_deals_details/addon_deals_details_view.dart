import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'addon_deals_details_viewmodel.dart';

class AddonDealsDetailsView extends StackedView<AddonDealsDetailsViewModel> {
  final DealModel deal;
  const AddonDealsDetailsView({super.key, required this.deal});

  @override
  Widget builder(BuildContext context, AddonDealsDetailsViewModel viewModel,
      Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: "addons.deals.details.title".tr(),
        previousPageTitle: "addons.deals.details.previouspagetitle".tr(),
      ),
      extendBody: true,
      extendBodyBehindAppBar: true,
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            Text(
              deal.name,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            Text(
              deal.commercialSector,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 15,
                color: Colors.black.withOpacity(.2),
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            if (deal.details != null) ...[
              _DealBlock(
                title: "addons.deals.details.subblock.details.title".tr(),
                content: deal.details,
              ),
            ],
            const SizedBox(height: 16),
            if (deal.who != null) ...[
              _DealBlock(
                title: "addons.deals.details.subblock.who.title".tr(),
                content: deal.getWho(),
              ),
            ],
            const SizedBox(height: 16),
            if (deal.howToGet != null) ...[
              _DealBlock(
                title: "addons.deals.details.subblock.howtoget.title".tr(),
                content: deal.howToGet,
              ),
            ],
            const SizedBox(height: 16),
            if (deal.attachment != null && deal.attachment!.isNotEmpty) ...[
              ElevatedButton(
                onPressed: () {
                  // Handle attachment opening here
                },
                child: const Text("Open deal attachment"),
              ),
            ],
            const SizedBox(height: 16),
            if (deal.link != null && deal.link!.isNotEmpty) ...[
              ElevatedButton(
                onPressed: () {
                  // Handle link opening here
                },
                child: const Text("Open deal information link"),
              ),
            ],
            if (viewModel.dealsContact != null)
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
                    if (viewModel.dealsContact!.name.isNotEmpty)
                      _DealBlock(
                        title: "Name",
                        content: viewModel.dealsContact!.name,
                      ),
                    const SizedBox(height: 16),
                    if (viewModel.dealsContact!.email.isNotEmpty)
                      _DealBlock(
                        title: "Email",
                        content: viewModel.dealsContact!.email,
                      ),
                    const SizedBox(height: 16),
                    if (viewModel.dealsContact!.phoneNumber != null &&
                        viewModel.dealsContact!.phoneNumber!.isNotEmpty)
                      _DealBlock(
                        title: "Phone",
                        content: viewModel.dealsContact!.phoneNumber,
                      ),
                    const SizedBox(height: 16),
                    if (viewModel.dealsContact!.note != null &&
                        viewModel.dealsContact!.note!.isNotEmpty)
                      _DealBlock(
                        title: "Note",
                        content: viewModel.dealsContact!.note,
                      ),
                  ],
                ),
              ),
            const SizedBox(height: 32),
            Text(
              "addons.deals.details.lastupdate".tr(namedArgs: {
                "date": DateFormat.yMMMd().format(deal.updated),
              }),
              textAlign: TextAlign.end,
              style: const TextStyle(
                fontSize: 14,
                color: Colors.grey,
              ),
            ),
            const SafeArea(child: SizedBox(height: 16)),
          ],
        ),
      ),
    );
  }

  @override
  AddonDealsDetailsViewModel viewModelBuilder(BuildContext context) =>
      AddonDealsDetailsViewModel(deal: deal);
}

class _DealBlock extends StatelessWidget {
  final String? title;
  final String? content;
  const _DealBlock({this.title, this.content});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 8),
          child: Text(
            title ?? "",
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.bold,
            ),
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(15),
            color: Colors.white,
          ),
          child: SelectableText(
            content ?? "",
          ),
        ),
      ],
    );
  }
}
