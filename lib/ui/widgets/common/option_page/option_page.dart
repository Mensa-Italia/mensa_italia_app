import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

import 'option_page_model.dart';

class OptionPage extends StackedView<OptionPageModel> {
  const OptionPage({super.key});

  @override
  Widget builder(BuildContext context, OptionPageModel viewModel, Widget? child) {
    return CustomScrollView(
      slivers: [
        getAppBarSliverPlatform(
          title: "views.settings.title".tr(),
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.list(
          children: [
            const SizedBox(height: 20),
            _SettingContainer(
              key: const ValueKey("settings:0"),
              children: [
                GestureDetector(
                  child: Row(
                    children: [
                      const SizedBox(width: 10),
                      CircleAvatar(
                        radius: 30,
                        backgroundColor: kcLightGrey,
                        backgroundImage: CachedNetworkImageProvider(
                          viewModel.user.avatar,
                          maxHeight: 180,
                          maxWidth: 180,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            AutoSizeText(
                              viewModel.user.name,
                              style: const TextStyle(
                                color: Colors.black,
                                fontWeight: FontWeight.bold,
                                fontSize: 16,
                                height: 1.2,
                              ),
                            ),
                            AutoSizeText(
                              viewModel.user.email,
                              style: const TextStyle(
                                color: Colors.black,
                                fontWeight: FontWeight.normal,
                                fontSize: 14,
                                height: 1.2,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SettingContainer(
              key: const ValueKey("settings:0"),
              children: [
                /*_OptionTile(
                  title: "Gestisci metodi di pagamento",
                  icon: EneftyIcons.card_outline,
                  onTap: viewModel.openPaymentMethodManager,
                  color: Colors.green,
                ),*/
                _OptionTile(
                  title: "views.settings.tile.renewmembership.title".tr(),
                  trailing: DateFormat.yMMMd().format(viewModel.user.expireMembership),
                  icon: EneftyIcons.coin_2_bold,
                  onTap: viewModel.renewSubscription,
                  color: Colors.orange,
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SettingContainer(
              key: const ValueKey("settings:1"),
              children: [
                _OptionTile(
                  title: "views.settings.tile.notification.title".tr(),
                  icon: EneftyIcons.notification_outline,
                  onTap: viewModel.openNotificationSettings,
                  color: Colors.red,
                ),
                _OptionTile(
                  title: "views.settings.tile.calendar.title".tr(),
                  subtitle: "views.settings.tile.calendar.description".tr(),
                  icon: EneftyIcons.calendar_2_outline,
                  onTap: viewModel.openCalendarLinker,
                  color: Colors.purple,
                ),
                _OptionTile(
                  title: "views.settings.tile.changepassword.title".tr(),
                  icon: EneftyIcons.lock_outline,
                  onTap: viewModel.changePassword,
                  color: Colors.blue,
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SettingContainer(
              key: const ValueKey("settings:2"),
              children: [
                _OptionTile(
                  title: "views.settings.tile.leavereview.title".tr(),
                  icon: EneftyIcons.star_outline,
                  onTap: viewModel.openReview,
                  color: Colors.orangeAccent,
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SettingContainer(
              key: const ValueKey("settings:3"),
              children: [
                _OptionTile(
                  title: "views.settings.tile.privacypolicy.title".tr(),
                  icon: EneftyIcons.security_safe_outline,
                  onTap: viewModel.openPrivacyPolicy,
                  color: Colors.green,
                ),
                _OptionTile(
                  title: "views.settings.tile.logout.title".tr(),
                  icon: EneftyIcons.logout_outline,
                  onTap: viewModel.logout,
                  color: Colors.red,
                ),
              ],
            ),
            const SizedBox(height: 100),
            Opacity(
              opacity: .3,
              child: Text(
                "Created by Matteo Sipione\nVersion: ${viewModel.version}",
                textAlign: TextAlign.center,
              ),
            ),
          ],
        ),
        const SliverSafeArea(sliver: SliverPadding(padding: EdgeInsets.only(bottom: 10))),
      ],
    );
  }

  @override
  OptionPageModel viewModelBuilder(BuildContext context) => OptionPageModel();
}

class _OptionTile extends StatelessWidget {
  final String title;
  final IconData icon;
  final String subtitle;
  final String trailing;
  final Function()? onTap;
  final Color color;
  const _OptionTile({
    this.title = "Logout",
    this.subtitle = "",
    this.trailing = "",
    this.icon = EneftyIcons.logout_outline,
    this.onTap,
    this.color = Colors.red,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      dense: true,
      title: Text(
        title,
        style: const TextStyle(
          color: Colors.black,
          fontWeight: FontWeight.normal,
          fontSize: 16,
          height: 0,
        ),
      ),
      subtitle: subtitle.isEmpty
          ? null
          : AutoSizeText(
              subtitle,
              style: const TextStyle(
                color: Colors.black,
                fontWeight: FontWeight.normal,
                fontSize: 12,
                height: 0,
              ),
              minFontSize: 0,
              maxLines: 1,
            ),
      leading: getPlatformIcon(),
      visualDensity: const VisualDensity(horizontal: -4, vertical: -4),
      contentPadding: const EdgeInsets.symmetric(horizontal: 10),
      trailing: trailing.isEmpty
          ? null
          : Text.rich(
              TextSpan(children: [
                TextSpan(text: trailing),
              ]),
              style: const TextStyle(
                color: Colors.black,
                fontWeight: FontWeight.normal,
                fontSize: 14,
              ),
            ),
    );
  }

  Widget getPlatformIcon() {
    if (Theme.of(StackedService.navigatorKey!.currentContext!).platform == TargetPlatform.iOS) {
      return Container(
        padding: const EdgeInsets.all(5),
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(5),
        ),
        child: Icon(icon, size: 20, color: Colors.white),
      );
    } else {
      return Icon(icon);
    }
  }
}

class _SettingContainer extends StatelessWidget {
  final List<Widget> children;
  const _SettingContainer({super.key, required this.children});

  @override
  Widget build(BuildContext context) {
    final isiOS = Theme.of(context).platform == TargetPlatform.iOS;
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.all(8).copyWith(right: 0, left: 2),
      decoration: BoxDecoration(
        color: isiOS ? Colors.white : Colors.transparent,
        borderRadius: BorderRadius.circular(10),
      ),
      child: ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: children.length,
        padding: EdgeInsets.zero,
        itemBuilder: (context, index) => children[index],
        separatorBuilder: (context, index) => Divider(
          indent: isiOS ? 50 : 0,
          color: isiOS ? kcLightGrey.withOpacity(.2) : Colors.transparent,
        ),
      ),
    );
  }
}
