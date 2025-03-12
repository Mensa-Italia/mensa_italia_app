import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

import 'notification_view_viewmodel.dart';

class NotificationViewView extends StackedView<NotificationViewViewModel> {
  const NotificationViewView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, NotificationViewViewModel viewModel, Widget? child) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          getAppBarSliverPlatform(
            title: "views.notificaiton.title".tr(),
            previousPageTitle: "Back",
            trailings: [
              IconButton(
                icon: Icon(EneftyIcons.setting_2_outline),
                onPressed: viewModel.goToSettings,
                iconSize: 24,
              ),
            ],
          ),
          if (viewModel.notifications.isEmpty && !viewModel.isBusy)
            SliverFillRemaining(
              child: Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      "views.notificaiton.no_notifications".tr(),
                    ),
                  ],
                ),
              ),
            ),
          if (viewModel.isBusy)
            SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(),
              ),
            ),
          if (viewModel.notifications.isNotEmpty && !viewModel.isBusy)
            SliverList.builder(
              itemBuilder: (context, index) {
                final notificaiton = viewModel.notifications[index];
                return ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
                  title: Text(notificaiton.title),
                  subtitle: Text(notificaiton.description),
                  leading: AspectRatio(
                    aspectRatio: 1,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Theme.of(context).primaryColor.withOpacity(.1),
                        shape: BoxShape.circle,
                      ),
                      alignment: Alignment.center,
                      child: Icon(EneftyIcons.ticket_outline),
                    ),
                  ),
                  onTap: () {
                    handleNotificationActions(
                      notificaiton.data,
                    );
                  },
                );
              },
              itemCount: viewModel.notifications.length,
            ),
          SliverSafeArea(sliver: SliverToBoxAdapter(child: SizedBox(height: 100))),
        ],
      ),
    );
  }

  IconData getBasedOnNotification(NotificationModel notificaiton) {
    if (notificaiton.data["type"] == "event") {
      return EneftyIcons.ticket_outline;
    }
    if (notificaiton.data["type"] == "single_document") {
      return EneftyIcons.document_2_outline;
    }
    if (notificaiton.data["type"] == "multiple_documents") {
      return EneftyIcons.document_2_outline;
    }
    return EneftyIcons.notification_outline;
  }

  @override
  NotificationViewViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      NotificationViewViewModel();
}
