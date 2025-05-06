import 'package:dart_rss/dart_rss.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/addon.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/services/notify_sse.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:url_launcher/url_launcher_string.dart';

class MembershipPageModel extends MasterModel {
  RssItem? lastBlogPost;
  SigModel? randomoSig;
  EventModel? nextEvent;
  List<AddonModel> addons = [];
  List<String> favsAddons = [];

  int get unseenNotifications => NotifySSE().unseenNotifications;

  MembershipPageModel() {
    ScraperApi().getBlog().then((value) {
      lastBlogPost = value.items.first;
      rebuildUi();
    });
    Api().getRandomSig().then((value) {
      randomoSig = value;
      rebuildUi();
    });
    Api().getFirstNextEvent().then((value) {
      nextEvent = value;
      rebuildUi();
    });

    NotifySSE().addListener(rebuildUi);

    SharedPreferences.getInstance().then((prefs) async {
      favsAddons.clear();
      if (!allowTestMakerAddon()) {
        await prefs.setStringList(
            "addons_fav",
            (prefs.getStringList("addons_fav") ?? [])
              ..removeWhere(
                  (element) => element.startsWith("INTERNAL:testmakers")));
      }
      favsAddons.addAll(prefs.getStringList("addons_fav") ?? []);
      Api().getAddons().then((value) {
        addons.clear();

        List<String> toRemove = [];
        for (var favsAddon in favsAddons) {
          if (favsAddon.startsWith("EXTERNAL:")) {
            if (!value
                .any((element) => "EXTERNAL:${element.id}" == favsAddon)) {
              toRemove.add(favsAddon);
            }
          }
        }
        for (var favsAddon in toRemove) {
          favsAddons.remove(favsAddon);
        }
        prefs.setStringList("addons_fav", favsAddons);

        for (final addon in value) {
          if (favsAddons.contains("EXTERNAL:${addon.id}")) {
            addons.add(addon);
          }
        }
        rebuildUi();
      });
    });
  }

  bool hasInternalAddon(String addonName) {
    return favsAddons.contains("INTERNAL:${addonName.toLowerCase()}");
  }

  IconData getIconForInternalAddon(String addonName) {
    switch (addonName.toLowerCase()) {
      case "contacts":
        return EneftyIcons.bookmark_outline;
      case "testmakers":
        return EneftyIcons.teacher_outline;
      case "documents":
        return EneftyIcons.document_cloud_outline;
      case "deals":
        return EneftyIcons.moneys_outline;
      case "tableport":
        return EneftyIcons.global_outline;
      case 'boutique':
        return EneftyIcons.shop_outline;
      default:
        return EneftyIcons.bookmark_outline;
    }
  }

  Function() openExternalAddon(AddonModel addon) {
    return () {
      navigationService.navigateToExternalAddonWebviewView(
          addonID: addon.id, addonURL: addon.url);
    };
  }

  Function() openInternalAddon(String addonName) {
    return () {
      switch (addonName.toLowerCase()) {
        case "contacts":
          navigationService.navigateToAddonContactsView();
          break;
        case "testmakers":
          navigationService.navigateToAddonTestAssistantView();
          break;
        case "documents":
          navigationService.navigateToAddonAreaDocumentsView();
          break;
        case "deals":
          navigationService.navigateToAddonDealsView();
          break;
        case "tableport":
          navigationService.navigateToAddonStampView();
          break;
        case 'boutique':
          navigationService.navigateToAddonBoutiqueView();
          break;
        default:
          break;
      }
    };
  }

  openExternalEvent(EventModel nextEvent) {
    return () async {
      if (await canLaunchUrlString(nextEvent.infoLink.trim())) {
        launchUrlString(
          nextEvent.infoLink.trim(),
        );
      }
    };
  }

  openExternalSig(SigModel lastSig) {
    return () async {
      if (await canLaunchUrlString(lastSig.link.trim())) {
        launchUrlString(
          lastSig.link.trim(),
          mode: LaunchMode.externalApplication,
        );
      }
    };
  }

  openExternalBlog(RssItem lastBlogPost) {
    return () async {
      if (await canLaunchUrlString(lastBlogPost.link!.trim())) {
        launchUrlString(
          lastBlogPost.link!.trim(),
        );
      }
    };
  }

  void openNotifications() {
    navigationService.navigateToNotificationViewView();
  }

  @override
  void dispose() {
    NotifySSE().addListener(rebuildUi);
    super.dispose();
  }
}
