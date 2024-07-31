import 'package:faker/faker.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'addon_contacts_viewmodel.dart';

class AddonContactsView extends StackedView<AddonContactsViewModel> {
  const AddonContactsView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, AddonContactsViewModel viewModel, Widget? child) {
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
                  'Contacts',
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
              'Contacts',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            alwaysShowMiddle: false,
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          SliverList.separated(
            itemCount: viewModel.contacts.length,
            itemBuilder: (context, index) {
              bool firstCharIsDifferent = index == 0 || viewModel.contacts[index][0] != viewModel.contacts[index - 1][0];
              if (firstCharIsDifferent) {
                return Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 20),
                      child: Row(
                        children: [
                          Text(
                            viewModel.contacts[index][0],
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500, color: Colors.grey),
                          ),
                        ],
                      ),
                    ),
                    const Divider(height: 0, endIndent: 20, indent: 20),
                    _ContactsTile(
                      fullName: viewModel.contacts[index],
                      avatar: "https://picsum.photos/200/300?random=$index",
                    ),
                  ],
                );
              } else {
                return _ContactsTile(
                  fullName: viewModel.contacts[index],
                  avatar: "https://picsum.photos/200/300?random=$index",
                );
              }
            },
            separatorBuilder: (context, index) => const Divider(height: 0, endIndent: 20, indent: 20),
          ),
          const SliverSafeArea(
            sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10)),
          ),
        ],
      ),
    );
  }

  @override
  AddonContactsViewModel viewModelBuilder(BuildContext context) => AddonContactsViewModel();
}

class _ContactsTile extends ViewModelWidget<AddonContactsViewModel> {
  final String fullName;
  final String avatar;

  const _ContactsTile({
    Key? key,
    required this.fullName,
    required this.avatar,
  }) : super(key: key);

  @override
  Widget build(BuildContext context, AddonContactsViewModel viewModel) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 20),
      visualDensity: VisualDensity.compact,
      dense: true,
      minLeadingWidth: 0,
      minVerticalPadding: 0,
      title: Text.rich(
        TextSpan(
          text: fullName.split(' ').first,
          style: const TextStyle(fontWeight: FontWeight.w700),
          children: [
            const TextSpan(text: ' '),
            TextSpan(
              text: fullName.replaceFirst(" ", "~~~").split('~~~').last,
              style: const TextStyle(fontWeight: FontWeight.w400),
            ),
          ],
        ),
        style: const TextStyle(fontSize: 16),
      ),
      onTap: () {},
    );
  }
}
