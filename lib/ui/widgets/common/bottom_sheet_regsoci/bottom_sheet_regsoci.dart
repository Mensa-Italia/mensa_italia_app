import 'dart:ui';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'bottom_sheet_regsoci_model.dart';

final AutoSizeGroup _autoSizeGroup = AutoSizeGroup();

class BottomSheetRegsoci extends StackedView<BottomSheetRegsociModel> {
  final RegSociModel regSoci;
  const BottomSheetRegsoci({super.key, required this.regSoci});

  @override
  Widget builder(
      BuildContext context, BottomSheetRegsociModel viewModel, Widget? child) {
    print(regSoci.image);
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
              children: [
                if (regSoci.birthDate != null)
                  Row(
                    children: [
                      Expanded(child: SizedBox()),
                      Container(
                        margin: const EdgeInsets.symmetric(
                            vertical: 10, horizontal: 10),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 5),
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: Colors.purple,
                          borderRadius: BorderRadius.circular(300),
                        ),
                        child: Text.rich(
                          TextSpan(children: [
                            WidgetSpan(
                              child: Icon(
                                EneftyIcons.cake_outline,
                                color: Colors.white,
                                size: 22,
                              ),
                              alignment: PlaceholderAlignment.middle,
                            ),
                            TextSpan(
                              text: " ${DateFormat("dd MMMM yyyy").format(
                                regSoci.birthDate ?? DateTime.now(),
                              )}",
                            ),
                          ]),
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                            height: 1,
                          ),
                        ),
                      ),
                    ],
                  ),
                const SizedBox(height: 20),
                if (regSoci.image ==
                    "https://www.cloud32.it/Associazioni/img/Uomo-1.png")
                  Container(
                    width: MediaQuery.of(context).size.width / 3,
                    height: MediaQuery.of(context).size.width / 3,
                    decoration: const BoxDecoration(
                      shape: BoxShape.circle,
                      image: DecorationImage(
                        image: AssetImage("assets/images/placeholder.png"),
                        fit: BoxFit.cover,
                      ),
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Color.fromARGB(255, 183, 183, 183),
                          Color.fromARGB(255, 125, 125, 125),
                        ],
                      ),
                    ),
                    child: Center(
                      child: Text(
                        regSoci.firstWords,
                        style: const TextStyle(
                          fontSize: 50,
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  )
                else
                  CachedNetworkImage(
                    imageUrl: regSoci.image,
                    imageBuilder: (context, imageProvider) => Container(
                      width: MediaQuery.of(context).size.width / 3,
                      height: MediaQuery.of(context).size.width / 3,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        image: DecorationImage(
                          image: imageProvider,
                          fit: BoxFit.cover,
                        ),
                      ),
                    ),
                  ),
                const SizedBox(height: 20),
                AutoSizeText(
                  capitalization(regSoci.name),
                  style: const TextStyle(
                    color: Colors.black,
                    fontWeight: FontWeight.bold,
                    fontSize: 20,
                  ),
                  minFontSize: 0,
                  maxLines: 1,
                ),
                AutoSizeText(
                  "${regSoci.id}",
                  style: const TextStyle(
                    color: Colors.black,
                    fontSize: 14,
                  ),
                  minFontSize: 0,
                  maxLines: 1,
                ),
                const SizedBox(height: 50),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    if (viewModel.hasPhoneNumbers())
                      _squareButtoon(
                        icon: const Icon(EneftyIcons.call_bold,
                            color: Colors.black),
                        text: "addons.contacts.infos.call".tr(),
                        onPressed: viewModel.linkToPhone,
                      ),
                    if (viewModel.hasPhoneNumbers())
                      _squareButtoon(
                        icon: const Icon(EneftyIcons.message_bold,
                            color: Colors.black),
                        text: "addons.contacts.infos.text".tr(),
                        onPressed: viewModel.linkToMessage,
                      ),
                    if (viewModel.hasEmail())
                      _squareButtoon(
                        icon: const Icon(EneftyIcons.sms_bold,
                            color: Colors.black),
                        text: "addons.contacts.infos.email".tr(),
                        onPressed: viewModel.linkToEmail,
                      ),
                    if (viewModel.hasWebsite())
                      _squareButtoon(
                        icon: const Icon(EneftyIcons.link_bold,
                            color: Colors.black),
                        text: "addons.contacts.infos.website".tr(),
                        onPressed: viewModel.linkToWebsite,
                      ),
                    _squareButtoon(
                      icon: const Icon(EneftyIcons.profile_bold,
                          color: Colors.black),
                      text: "addons.contacts.infos.profile".tr(),
                      onPressed: viewModel.linkToProfile,
                    ),
                  ],
                ),
                const SizedBox(height: 10),
              ],
            ),
          ),
        ),
      ),
    );
  }

  String capitalization(String text) {
    var textList = text.split(" ");
    if (textList.length == 1) {
      if (textList[0].length > 1) {
        textList[0] = textList[0][0].toUpperCase() +
            textList[0].substring(1).toLowerCase();
      } else {
        textList[0] = textList[0].toUpperCase();
      }
    } else {
      for (var i = 0; i < textList.length; i++) {
        if (textList[i].length > 1) {
          textList[i] = textList[i][0].toUpperCase() +
              textList[i].substring(1).toLowerCase();
        } else {
          textList[i] = textList[i].toUpperCase();
        }
      }
    }
    return textList.join(" ");
  }

  @override
  BottomSheetRegsociModel viewModelBuilder(BuildContext context) =>
      BottomSheetRegsociModel(regSoci: regSoci);
}

class _squareButtoon extends StatelessWidget {
  final Widget icon;
  final String text;
  final Function()? onPressed;
  const _squareButtoon(
      {required this.icon, required this.text, this.onPressed});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onPressed,
      child: Container(
        width: MediaQuery.of(context).size.width / 6,
        height: MediaQuery.of(context).size.width / 6,
        margin: EdgeInsets.all(MediaQuery.of(context).size.width / 100),
        alignment: Alignment.center,
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(.4),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: Colors.white.withOpacity(.4), width: 1),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Expanded(child: icon),
            AutoSizeText(
              text,
              style: const TextStyle(
                  color: Colors.black, fontWeight: FontWeight.bold),
              minFontSize: 0,
              maxLines: 1,
              group: _autoSizeGroup,
            ),
          ],
        ),
      ),
    );
  }
}
