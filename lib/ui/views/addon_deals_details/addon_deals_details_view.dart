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
        title: "Details",
        previousPageTitle: "Deals",
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
                title: "Details",
                content: deal.details,
              ),
            ],
            const SizedBox(height: 16),
            if (deal.who != null) ...[
              _DealBlock(
                title: "Who",
                content: deal.getWho(),
              ),
            ],
            const SizedBox(height: 16),
            if (deal.howToGet != null) ...[
              _DealBlock(
                title: "How to get",
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
            const SizedBox(height: 32),
            Text(
              "Last update: ${DateFormat.yMMMd().format(deal.updated)}",
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
      AddonDealsDetailsViewModel();
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
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(15),
            color: Colors.white,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 8),
              SelectableText(
                content ?? "",
              ),
              const SizedBox(height: 16),
            ],
          ),
        ),
      ],
    );
  }
}
