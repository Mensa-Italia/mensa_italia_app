import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AddonPageModel extends MasterModel {
  final List<AddonModel> _storedAddons = [];
  final List<AddonModel> addons = [];
  final List<String> favsAddons = [];

  String searchText = "";

  bool isSearching(String check) {
    return check.toLowerCase().contains(searchText.toLowerCase()) || searchText.isEmpty;
  }

  TextEditingController searchController = TextEditingController();

  ScrollController scrollController = ScrollController();

  AddonPageModel() {
    SharedPreferences.getInstance().then((prefs) async {
      favsAddons.clear();
      if (!allowTestMakerAddon()) {
        await prefs.setStringList("addons_fav", (prefs.getStringList("addons_fav") ?? [])..removeWhere((element) => element.startsWith("INTERNAL:testmakers")));
      }
      favsAddons.addAll(prefs.getStringList("addons_fav") ?? []);
      Api().getAddons().then((value) {
        _storedAddons.clear();
        _storedAddons.addAll(value);

        List<String> toRemove = [];
        for (var favsAddon in favsAddons) {
          if (favsAddon.startsWith("EXTERNAL:")) {
            if (!_storedAddons.any((element) => "EXTERNAL:${element.id}" == favsAddon)) {
              toRemove.add(favsAddon);
            }
          }
        }
        for (var favsAddon in toRemove) {
          favsAddons.remove(favsAddon);
        }
        prefs.setStringList("addons_fav", favsAddons);
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

  void search(String value) {
    searchText = value;
    addons.clear();
    addons.addAll(_storedAddons.where((element) => isSearching(element.name.toLowerCase())));
    rebuildUi();
  }

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
