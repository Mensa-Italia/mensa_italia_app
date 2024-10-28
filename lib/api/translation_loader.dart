import 'dart:ui';
import 'package:dio/dio.dart';
import 'package:easy_localization/easy_localization.dart';

class TranslationLoader extends AssetLoader {
  const TranslationLoader();

  static const tolgeeKey = "tgpak_geydomjsl42wsmrwom2wooljgbyhezdrmnyg2zzzge4wenbrgbsa";

  @override
  Future<Map<String, dynamic>?> load(String path, Locale locale) async {
    try {
      Uri buildUri = Uri.parse('https://app.tolgee.io/v2/projects/export').replace(queryParameters: {
        'ak': tolgeeKey,
        'languages': locale.languageCode,
        'zip': 'false',
        'format': 'JSON',
      });
      print(buildUri.toString());
      return await Dio().getUri(buildUri).then((value) => value.data);
    } catch (_) {}
    return {};
  }

  static Future<List<Locale>> getLocalizationList() async {
    Uri buildUri = Uri.parse('https://app.tolgee.io/v2/projects/languages').replace(queryParameters: {
      'ak': tolgeeKey,
      "size": "1000",
    });
    return await Dio().getUri(buildUri).then((value) => value.data['_embedded']['languages'].map<Locale>((e) => Locale(e['tag'])).toList());
  }
}
