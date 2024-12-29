import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/custom_scroll_view.dart';
import 'package:stacked/stacked.dart';

import 'addon_contacts_viewmodel.dart';

class AddonContactsView extends StackedView<AddonContactsViewModel> {
  const AddonContactsView({super.key});

  @override
  Widget builder(BuildContext context, AddonContactsViewModel viewModel, Widget? child) {
    return Scaffold(
      body: getCustomScrollViewPlatform(
        controller: viewModel.scrollController,
        slivers: [
          getAppBarSliverPlatform(
            title: "addons.contacts.view.title".tr(),
            previousPageTitle: "addons.contacts.view.previouspagetitle".tr(),
            searchBarActions: SearchBarActions(
              onChanged: viewModel.search,
              controller: viewModel.searchController,
              onSubmitted: viewModel.search,
              hintText: "addons.contacts.search.textfield".tr(),
            ),
            trailings: (viewModel.isCompleted)
                ? null
                : [
                    Material(
                      color: Colors.transparent,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(
                          color: Colors.grey.withOpacity(.2),
                          borderRadius: BorderRadius.circular(100),
                        ),
                        child: Row(
                          children: [
                            CircularProgressIndicator.adaptive(),
                            SizedBox(width: 5),
                            Text("addons.contacts.view.loading".tr(), style: TextStyle(color: Colors.grey, fontSize: 14)),
                          ],
                        ),
                      ),
                    )
                  ],
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          SliverList.separated(
            itemCount: viewModel.countMembers,
            itemBuilder: (context, index) {
              final contact = viewModel.getElementAt(index);
              final contactPrevious = viewModel.getElementAt(index - 1);
              bool firstCharIsDifferent = index == 0 || contact.name[0] != contactPrevious.name[0];
              if (firstCharIsDifferent) {
                return Column(
                  key: ValueKey("${contact.id}:column"),
                  children: [
                    Padding(
                      key: ValueKey("${contact.id}:padding"),
                      padding: const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 20),
                      child: Row(
                        children: [
                          Text(
                            contact.name[0],
                            style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w500, color: Colors.grey),
                          ),
                        ],
                      ),
                    ),
                    Divider(height: 0, endIndent: 20, indent: 20, key: ValueKey("${contact.id}:divider")),
                    _ContactsTile(
                      key: ValueKey(contact.id),
                      contact: contact,
                      onTap: viewModel.tapOnContact(index),
                    ),
                  ],
                );
              } else {
                return _ContactsTile(
                  key: ValueKey(contact.id),
                  contact: contact,
                  onTap: viewModel.tapOnContact(index),
                );
              }
            },
            separatorBuilder: (context, index) => Divider(
              key: ValueKey(index),
              height: 0,
              endIndent: 20,
              indent: 20,
            ),
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
  final RegSociModel contact;
  final Function() onTap;

  const _ContactsTile({
    super.key,
    required this.contact,
    required this.onTap,
  });

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
          text: capitalization(contact.name.split(' ').first).trim(),
          style: const TextStyle(fontWeight: FontWeight.w700),
          children: [
            const TextSpan(text: ' '),
            TextSpan(
              text: capitalization(contact.name.replaceFirst(" ", "~~~").split('~~~').last).trim(),
              style: const TextStyle(fontWeight: FontWeight.w400),
            ),
          ],
        ),
        style: const TextStyle(fontSize: 16),
      ),
      onTap: onTap,
    );
  }

  String capitalization(String text) {
    var textList = text.split(" ");
    if (textList.length == 1) {
      if (textList[0].length > 1) {
        textList[0] = textList[0][0].toUpperCase() + textList[0].substring(1).toLowerCase();
      } else {
        textList[0] = textList[0].toUpperCase();
      }
    } else {
      for (var i = 0; i < textList.length; i++) {
        if (textList[i].length > 1) {
          textList[i] = textList[i][0].toUpperCase() + textList[i].substring(1).toLowerCase();
        } else {
          textList[i] = textList[i].toUpperCase();
        }
      }
    }
    return textList.join(" ");
  }
}
