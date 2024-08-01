import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AddonPageModel extends MasterModel {
  final List<AddonModel> addons = [];
  final List<String> favsAddons = [];

  TextEditingController searchController = TextEditingController();

  ScrollController scrollController = ScrollController();

  AddonPageModel() {
    SharedPreferences.getInstance().then((prefs) {
      favsAddons.clear();
      favsAddons.addAll(prefs.getStringList("addons_fav") ?? []);
      Api().getAddons().then((value) {
        addons.clear();
        addons.addAll(value);
        rebuildUi();
      });
    });
  }

  openAddon(AddonModel addon) {
    navigationService.navigateToExternalAddonWebviewView(addonID: addon.id);
  }

  openContacts() {
    navigationService.navigateToAddonContactsView();
  }

  openTestMakers() {
    navigationService.navigateToAddonTestAssistantView();
  }

  void search(String value) {}

  openDocuments() {
    navigationService.navigateToAddonAreaDocumentsView();
  }

  onStarTappedExternal(AddonModel addon) {
    SharedPreferences.getInstance().then((prefs) {
      final String addonID = "EXTERNAL:${addon.id}";
      List<String> favs = prefs.getStringList("addons_fav") ?? [];
      if (favs.contains(addonID)) {
        favs.remove(addonID);
      } else {
        if (favs.length >= 4) {
          dialogService.showDialog(
            title: "Error",
            description: "You can't have more than 4 favorites",
          );
          return;
        }
        favs.add(addonID);
      }
      prefs.setStringList("addons_fav", favs);
      favsAddons.clear();
      favsAddons.addAll(favs);
      rebuildUi();
    });
  }

  onStarTappedInternal(String addon) {
    SharedPreferences.getInstance().then((prefs) {
      final String addonID = "INTERNAL:${addon.toLowerCase()}";
      List<String> favs = prefs.getStringList("addons_fav") ?? [];
      if (favs.contains(addonID)) {
        favs.remove(addonID);
      } else {
        if (favs.length >= 4) {
          dialogService.showDialog(
            title: "Error",
            description: "You can't have more than 4 favorites",
          );
          return;
        }
        favs.add(addonID);
      }
      prefs.setStringList("addons_fav", favs);
      favsAddons.clear();
      favsAddons.addAll(favs);
      rebuildUi();
    });
  }

  IconData getStarIconExternal(AddonModel addon) {
    final String addonID = "EXTERNAL:${addon.id}";
    return favsAddons.contains(addonID) ? EneftyIcons.star_bold : EneftyIcons.star_outline;
  }

  IconData getStarIconInternal(String addon) {
    final String addonID = "INTERNAL:${addon.toLowerCase()}";
    return favsAddons.contains(addonID) ? EneftyIcons.star_bold : EneftyIcons.star_outline;
  }
}
