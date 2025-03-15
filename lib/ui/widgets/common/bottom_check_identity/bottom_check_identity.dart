import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:dio/dio.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/ex_app.dart';
import 'package:stacked/stacked.dart';

import 'bottom_check_identity_model.dart';

class BottomCheckIdentity extends StackedView<BottomCheckIdentityModel> {
  final String? notificationToRemove;
  final String urlToCall;
  final ExAppModel exApp;
  const BottomCheckIdentity({Key? key, required this.urlToCall, required this.exApp, this.notificationToRemove}) : super(key: key);

  @override
  Widget builder(BuildContext context, BottomCheckIdentityModel viewModel, Widget? child) {
    return ClipRRect(
      borderRadius: const BorderRadius.only(
        topLeft: Radius.circular(30),
        topRight: Radius.circular(30),
      ),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(.6),
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(30),
              topRight: Radius.circular(30),
            ),
            border: Border.all(color: Colors.white.withOpacity(.4), width: 2),
          ),
          child: SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 40),
                Text(
                  exApp.name ?? "",
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 40),
                Center(
                  child: CachedNetworkImage(
                    imageUrl: exApp.image ?? "",
                    height: 200,
                    width: 200,
                    fit: BoxFit.cover,
                  ),
                ),
                const SizedBox(height: 40),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Text(
                    "viewmodel.exApp.description".tr(
                      namedArgs: {
                        "name": exApp.name ?? "",
                      },
                    ),
                    style: const TextStyle(
                      fontSize: 16,
                    ),
                  ),
                ),
                const SizedBox(height: 40),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: ElevatedButton(
                    onPressed: () {
                      Api().addExtAppPermission(
                        exApp.id!,
                        permToAdd: ["CHECK_USER_EXISTENCE"],
                      ).then((_) {
                        Dio().post(urlToCall, data: {
                          "accepted": true,
                        }).then((value) {
                          if (notificationToRemove != null) {
                            Api().removeNotification(notificationToRemove!);
                          }
                          viewModel.navigationService.back();
                        });
                      });
                    },
                    child: Text("viewmodel.exApp.approve".tr()),
                  ),
                ),
                const SizedBox(height: 10),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: ElevatedButton(
                    onPressed: () {
                      Api().removeExtAppPermission(
                        exApp.id!,
                        permToRemove: ["CHECK_USER_EXISTENCE"],
                      ).then((_) {
                        Dio().post(urlToCall, data: {
                          "accepted": false,
                        }).then((value) {
                          if (notificationToRemove != null) {
                            Api().removeNotification(notificationToRemove!);
                          }
                          viewModel.navigationService.back();
                        });
                      });
                    },
                    style: ButtonStyle(
                      backgroundColor: MaterialStateProperty.all(Colors.black),
                    ),
                    child: Text("viewmodel.exApp.cancel".tr()),
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
  BottomCheckIdentityModel viewModelBuilder(BuildContext context) => BottomCheckIdentityModel();
}
