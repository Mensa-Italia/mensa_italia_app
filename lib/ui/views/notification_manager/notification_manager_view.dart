import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'notification_manager_viewmodel.dart';

class NotificationManagerView extends StackedView<NotificationManagerViewModel> {
  final String previousPageTitle;
  const NotificationManagerView({Key? key, required this.previousPageTitle}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    NotificationManagerViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
        appBar: CupertinoNavigationBar(
          previousPageTitle: previousPageTitle.tr(),
          middle: Text(
            viewModel.componentName.tr(),
            maxLines: 1,
          ),
        ),
        body: ListView(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 16),
              child: Text(
                "views.notification_manager.kind.events".tr(),
              ),
            ),
            const Divider(indent: 16, endIndent: 16),
            ...ListOfStates.map(
              (state) {
                return ListTile(
                  visualDensity: VisualDensity.compact,
                  dense: true,
                  title: Text(state),
                  trailing: CupertinoSwitch(
                    value: viewModel.hasState(state),
                    activeTrackColor: kcPrimaryColor,
                    onChanged: viewModel.changeState(state),
                  ),
                );
              },
            ),
          ],
        ));
  }

  @override
  NotificationManagerViewModel viewModelBuilder(BuildContext context) => NotificationManagerViewModel();
}
