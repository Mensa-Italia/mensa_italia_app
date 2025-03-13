import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:gpt_markdown/gpt_markdown.dart';
import 'package:mensa_italia_app/model/document.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'addon_area_documents_preview_viewmodel.dart';

class AddonAreaDocumentsPreviewView
    extends StackedView<AddonAreaDocumentsPreviewViewModel> {
  final DocumentModel document;
  const AddonAreaDocumentsPreviewView({super.key, required this.document});

  @override
  Widget builder(BuildContext context,
      AddonAreaDocumentsPreviewViewModel viewModel, Widget? child) {
    return Scaffold(
      bottomNavigationBar: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: BoxDecoration(
          color: Theme.of(context).colorScheme.background,
        ),
        child: SafeArea(
          child: ElevatedButton(
            onPressed: viewModel.onTapViewOriginal,
            child: Text("views.addons.documents_resume.view_orginal"
                .tr()
                .toUpperCase()),
          ),
        ),
      ),
      body: CustomScrollView(
        controller: viewModel.scrollController,
        slivers: [
          getAppBarSliverPlatform(
            title: "views.addons.documents_resume.title".tr(),
            previousPageTitle: "views.addons.documents.title".tr(),
          ),
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 20)
                .copyWith(top: 10, bottom: 0),
            sliver: SliverToBoxAdapter(
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.circular(10),
                ),
                clipBehavior: Clip.antiAlias,
                child: Row(
                  children: [
                    Padding(
                      padding: const EdgeInsets.all(8.0).copyWith(left: 20),
                      child: Icon(
                        EneftyIcons.warning_2_outline,
                        size: 35,
                        color: Colors.white,
                      ),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(10.0).copyWith(right: 20),
                        child: Text(
                          "views.addons.documents_resume.warning".tr(),
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (viewModel.isBusy)
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(),
              ),
            )
          else
            SliverPadding(
              padding:
                  const EdgeInsets.symmetric(horizontal: 20).copyWith(top: 30),
              sliver: SliverToBoxAdapter(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    GptMarkdown(
                      viewModel.documentElaboratedModel?.iaResume ?? "",
                    ),
                  ],
                ),
              ),
            ),
          SliverSafeArea(
              sliver: SliverToBoxAdapter(
            child: SizedBox(height: 50),
          )),
        ],
      ),
    );
  }

  @override
  AddonAreaDocumentsPreviewViewModel viewModelBuilder(BuildContext context) =>
      AddonAreaDocumentsPreviewViewModel(
        document: document,
      );
}
