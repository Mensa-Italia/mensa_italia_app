import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'addon_test_assistant_viewmodel.dart';

class AddonTestAssistantView extends StackedView<AddonTestAssistantViewModel> {
  const AddonTestAssistantView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, AddonTestAssistantViewModel viewModel, Widget? child) {
    return Scaffold(
      body: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        controller: viewModel.scrollController,
        anchor: 0.06,
        slivers: [
          CupertinoSliverNavigationBar(
            largeTitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Candidates',
                  style: TextStyle(fontWeight: FontWeight.w900),
                ),
                Container(
                  height: 40,
                  child: Padding(
                    padding: const EdgeInsets.only(right: 15, top: 3),
                    child: CupertinoSearchTextField(
                      onChanged: viewModel.search,
                      controller: viewModel.searchController,
                      prefixIcon: const Icon(CupertinoIcons.search),
                      onSubmitted: viewModel.search,
                    ),
                  ),
                ),
              ],
            ),
            stretch: true,
            previousPageTitle: "Addons",
            backgroundColor: Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
            border: null,
            middle: const Text(
              'Candidates',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            alwaysShowMiddle: false,
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          if (viewModel.testelabs.isEmpty)
            const SliverFillRemaining(
              child: SizedBox(
                height: 100,
                child: Center(
                  child: Text("No data found"),
                ),
              ),
            )
          else
            SliverList.separated(
              itemCount: viewModel.testelabs.length,
              itemBuilder: (context, index) {
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 10),
                  dense: true,
                  visualDensity: VisualDensity.compact,
                  title: Text(viewModel.testelabs[index].fullname),
                  subtitle: Text(viewModel.testelabs[index].typeOfTest + "\n" + viewModel.testelabs[index].getAvailableModality()),
                  trailing: Text(
                    viewModel.testelabs[index].status + "\n" + viewModel.testelabs[index].state,
                    textAlign: TextAlign.end,
                  ),
                  onTap: viewModel.tapOnCandidate(index),
                );
              },
              separatorBuilder: (context, index) => const Divider(),
            ),
          const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
        ],
      ),
    );
  }

  @override
  AddonTestAssistantViewModel viewModelBuilder(BuildContext context) => AddonTestAssistantViewModel();
}
