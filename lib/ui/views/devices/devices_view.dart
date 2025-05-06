import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'devices_viewmodel.dart';

class DevicesView extends StackedView<DevicesViewModel> {
  const DevicesView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    DevicesViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          getAppBarSliverPlatform(
            title: "views.devices.title".tr(),
            previousPageTitle: "views.settings.title".tr(),
          ),
          const SliverPadding(padding: EdgeInsets.all(5)),
          if (viewModel.isBusy)
            const SliverFillRemaining(
              child: Center(
                child: CircularProgressIndicator(),
              ),
            )
          else
            SliverList.builder(
              itemCount: viewModel.devices.length,
              itemBuilder: (context, index) {
                final device = viewModel.devices[index];
                return ListTile(
                  leading: AspectRatio(
                    aspectRatio: 1,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Theme.of(context).primaryColor.withOpacity(.1),
                        shape: BoxShape.circle,
                      ),
                      alignment: Alignment.center,
                      child: Icon(EneftyIcons.devices_outline),
                    ),
                  ),
                  title: Text(device.deviceName),
                  subtitle: device.firebaseId == Api().notificationToken
                      ? Text("views.devices.current_device".tr())
                      : null,
                  trailing: device.firebaseId != Api().notificationToken
                      ? IconButton(
                          icon: const Icon(Icons.delete_outline),
                          onPressed: () {
                            viewModel.deleteDevice(device);
                          },
                        )
                      : null,
                );
              },
            ),
        ],
      ),
    );
  }

  @override
  DevicesViewModel viewModelBuilder(
    BuildContext context,
  ) =>
      DevicesViewModel();
}
