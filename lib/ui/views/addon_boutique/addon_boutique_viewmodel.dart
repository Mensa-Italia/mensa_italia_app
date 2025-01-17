import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/boutique.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:url_launcher/url_launcher_string.dart';

class AddonBoutiqueViewModel extends MasterModel {

  ScrollController scrollController = ScrollController();
  List<BoutiqueModel> boutiques = [];
  List<BoutiqueModel> originalBoutiques = [];

  TextEditingController searchController = TextEditingController();

  AddonBoutiqueViewModel() {
    Api().getBoutiques().then((value) {
      boutiques.clear();
      boutiques.addAll(value);
      originalBoutiques.clear();
      originalBoutiques.addAll(value);
      rebuildUi();
    });
  }

  search(String p1) {
    if (p1.isEmpty) {
      boutiques.clear();
      boutiques.addAll(originalBoutiques);
    } else {
      boutiques.clear();
      boutiques.addAll(originalBoutiques.where((element) => element.name.toLowerCase().contains(p1.toLowerCase())));
    }
    rebuildUi();
  }

  openProduct(BoutiqueModel product) {
    navigationService.navigateToAddonBoutiqueProductView(product: product);
  }

  orderNow() {
    launchUrlString("https://forms.gle/TrERAc8XNWwFXvJR9");
  }
}
