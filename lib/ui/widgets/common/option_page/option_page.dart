import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'option_page_model.dart';

class OptionPage extends StackedView<OptionPageModel> {
  const OptionPage({super.key});

  @override
  Widget builder(BuildContext context, OptionPageModel viewModel, Widget? child) {
    return CustomScrollView(
      slivers: [
        CupertinoSliverNavigationBar(
          largeTitle: const Text('Settings', style: TextStyle(fontWeight: FontWeight.w900)),
          middle: const Text('Settings', style: TextStyle(fontWeight: FontWeight.bold)),
          alwaysShowMiddle: false,
          backgroundColor: Theme.of(context).scaffoldBackgroundColor.withOpacity(.9),
          border: null,
        ),
        const SliverPadding(padding: EdgeInsets.all(5)),
        SliverList.list(
          children: [
            const SizedBox(height: 20),
            _SettingContainer(
              children: [
                GestureDetector(
                  onTap: viewModel.editProfile,
                  child: Row(
                    children: [
                      CircleAvatar(
                        radius: 30,
                        backgroundColor: kcLightGrey,
                        backgroundImage: CachedNetworkImageProvider(viewModel.user.avatar),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            AutoSizeText(viewModel.user.name, style: const TextStyle(color: Colors.black, fontWeight: FontWeight.bold, fontSize: 16, height: 1.2)),
                            AutoSizeText(viewModel.user.email, style: const TextStyle(color: Colors.black, fontWeight: FontWeight.normal, fontSize: 14, height: 1.2)),
                          ],
                        ),
                      ),
                      Center(
                        child: IconButton(
                          icon: const Icon(Icons.chevron_right),
                          onPressed: viewModel.editProfile,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SettingContainer(
              children: [
                _OptionTile(
                  title: "Renew Membership",
                  subtitle: DateFormat.yMMMd().format(viewModel.user.expireMembership),
                  icon: EneftyIcons.card_outline,
                  onTap: viewModel.renewSubscription,
                  color: Colors.orange,
                ),
                _OptionTile(
                  title: "Change Password",
                  icon: EneftyIcons.lock_outline,
                  onTap: viewModel.changePassword,
                  color: Colors.blue,
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SettingContainer(
              children: [
                _OptionTile(
                  title: "Privacy Policy",
                  icon: EneftyIcons.security_safe_outline,
                  onTap: viewModel.openPrivacyPolicy,
                  color: Colors.green,
                ),
                _OptionTile(
                  title: "Logout",
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
  final Function()? onTap;
  final Color color;
  const _OptionTile({
    super.key,
    this.title = "Logout",
    this.subtitle = "",
    this.icon = EneftyIcons.logout_outline,
    this.onTap,
    this.color = Colors.red,
  });

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: onTap,
      dense: true,
      title: Text(title, style: const TextStyle(color: Colors.black, fontWeight: FontWeight.normal, fontSize: 16)),
      leading: Container(
        padding: const EdgeInsets.all(5),
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(5),
        ),
        child: Icon(icon, size: 20, color: Colors.white),
      ),
      visualDensity: const VisualDensity(horizontal: -4, vertical: -4),
      contentPadding: const EdgeInsets.symmetric(horizontal: 10),
      trailing: subtitle.isEmpty
          ? null
          : Text.rich(
              TextSpan(children: [
                TextSpan(text: subtitle),
              ]),
              style: const TextStyle(color: Colors.black, fontWeight: FontWeight.normal, fontSize: 14)),
    );
  }
}

class _SettingContainer extends StatelessWidget {
  final List<Widget> children;
  const _SettingContainer({super.key, required this.children});

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 20),
      padding: const EdgeInsets.all(8).copyWith(right: 0, left: 2),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
      ),
      child: ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: children.length,
        padding: EdgeInsets.zero,
        itemBuilder: (context, index) => children[index],
        separatorBuilder: (context, index) => Divider(
          indent: 50,
          color: kcLightGrey.withOpacity(.2),
        ),
      ),
    );
  }
}
