import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonPageModel extends MasterModel {
  final List<AddonModel> addons = [];

  AddonPageModel() {
    Api().getAddons().then((value) {
      addons.clear();
      addons.addAll(value);
      rebuildUi();
    });
  }

  openAddon() {
    navigationService.navigateToExternalAddonWebviewView(
        addonID: "oakk7cnnzpi5wlo");
  }

  openContacts() {
    navigationService.navigateToAddonContactsView();
  }

  openTestMakers() {
    navigationService.navigateToAddonTestAssistantView();
  }
}
