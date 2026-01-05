import 'dart:convert';
import 'dart:ui';
import 'package:dio/dio.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:mensa_italia_app/api/api.dart';

class TranslationLoader extends AssetLoader {
  const TranslationLoader();

  static Future<Map<String, String>> getConfigs() async {
    return Api().settings().then((configs) {
      return configs;
    });
  }

  @override
  Future<Map<String, dynamic>?> load(String path, Locale locale) async {
    try {
      Uri buildUri = Uri.parse(((await getConfigs())["i18n_structured_url"] ?? "").replaceAll('{locale}', locale.toLanguageTag()));
      return await Dio().getUri(buildUri).then((value) {
        String jsonString = value.toString();
        return jsonDecode(jsonString);
      }).catchError((e) {
        throw e;
      });
    } catch (_) {
      if (locale.toLanguageTag() != "en") {
        return await load(path, Locale("en"));
      }
    }
    return {};
  }

  static Future<List<Locale>> getLocalizationList() async {
    List<Locale> locales = ((await getConfigs())["languages"] ?? "").split(',').map((e) => Locale(e)).toList();
    return locales;
  }
}
