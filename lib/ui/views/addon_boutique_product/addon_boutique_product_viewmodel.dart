import 'package:carousel_slider/carousel_options.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:url_launcher/url_launcher_string.dart';

class AddonBoutiqueProductViewModel extends MasterModel {
  @override
  String componentName = "views.addons.boutiqueproduct.title";
  ValueNotifier<int> currentPage = ValueNotifier(0);

  onPageChanged(int index, CarouselPageChangedReason reason) {
    currentPage.value = index;
  }

  orderNow() {
    launchUrlString("https://forms.gle/TrERAc8XNWwFXvJR9");
  }
}
