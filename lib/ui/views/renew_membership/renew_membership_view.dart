import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:stacked/stacked.dart';

import 'renew_membership_viewmodel.dart';

class RenewMembershipView extends StackedView<RenewMembershipViewModel> {
  const RenewMembershipView({super.key});

  @override
  Widget builder(
      BuildContext context, RenewMembershipViewModel viewModel, Widget? child) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color.fromARGB(255, 252, 226, 248),
              Color.fromARGB(255, 191, 212, 252),
            ],
          ),
        ),
        child: SingleChildScrollView(
          child: ConstrainedBox(
            constraints: BoxConstraints(
              minHeight: MediaQuery.of(context).size.height,
            ),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(EneftyIcons.card_slash_outline,
                        size: 100, color: Colors.red),
                    SizedBox(height: 20),
                    Text(
                      """Your membership has expired.
Please renew it using the button
below.""",
                      style: TextStyle(
                        color: Colors.red,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 50),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      ElevatedButton(
                        onPressed: viewModel.goToRenewMembershipWebview,
                        child: const Text("RENEW MEMBERSHIP"),
                      ),
                      const SizedBox(height: 20),
                      Center(
                        child: TextButton.icon(
                          onPressed: viewModel.logout,
                          label: const Text("LOGOUT"),
                          icon: const Icon(EneftyIcons.logout_outline),
                          style: TextButton.styleFrom(
                            foregroundColor: Colors.red,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  RenewMembershipViewModel viewModelBuilder(BuildContext context) =>
      RenewMembershipViewModel();
}
