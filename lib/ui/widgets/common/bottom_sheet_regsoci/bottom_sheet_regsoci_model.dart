import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:url_launcher/url_launcher_string.dart';

class BottomSheetRegsociModel extends MasterModel {
  final RegSociModel regSoci;
  Map<String, String> deepData = {};

  BottomSheetRegsociModel({required this.regSoci}) {
    ScraperApi().getRegSocioDeepData(regSoci.linkToFullProfile).then((value) {
      deepData = value;
      rebuildUi();
    });
  }

  bool hasPhoneNumbers() {
    return deepData.containsKey("Telefono:") || deepData.containsKey("Cellulare:");
  }

  String getPhoneNumber() {
    if (deepData.containsKey("Cellulare:")) {
      return deepData["Cellulare:"]!;
    } else {
      return deepData["Telefono:"]!;
    }
  }

  linkToPhone() async {
    String phoneNumber = getPhoneNumber();
    String url = "tel:$phoneNumber";
    if (await canLaunchUrlString(url)) {
      await launchUrlString(url);
    } else {
      throw 'Could not launch $url';
    }
  }

  linkToMessage() async {
    String phoneNumber = getPhoneNumber();
    String url = "sms:$phoneNumber";
    if (await canLaunchUrlString(url)) {
      await launchUrlString(url);
    } else {
      throw 'Could not launch $url';
    }
  }

  bool hasEmail() {
    return deepData.containsKey("E-mail:");
  }

  String getEmail() {
    return deepData["E-mail:"]!;
  }

  linkToEmail() async {
    String email = getEmail();
    String url = "mailto:$email";
    if (email.contains("mailto:")) {
      url = email;
    }
    print(url);
    if (await canLaunchUrlString(url)) {
      await launchUrlString(url);
    } else {
      throw 'Could not launch $url';
    }
  }

  bool hasWebsite() {
    return deepData.containsKey("Sito:");
  }

  String getWebsite() {
    return deepData["Sito:"]!;
  }

  linkToWebsite() async {
    String website = getWebsite();
    String url = website;
    if (await canLaunchUrlString(url)) {
      await launchUrlString(url);
    } else {
      throw 'Could not launch $url';
    }
  }

  linkToProfile() async {
    String url = regSoci.linkToFullProfile;
    await navigationService.back();
    navigationService.navigateToGenericWebviewView(
      url: url,
      title: "Profile",
      previousPageTitle: "Contacts",
    );
  }
}
