import 'package:freezed_annotation/freezed_annotation.dart';

getDataFromExpanded(Map<dynamic, dynamic> json, String key) {
  try {
    if (json[key] is String) {
      return json["expand"][key];
    } else {
      return json[key];
    }
  } catch (_) {
    return null;
  }
}

DateTime getDateTimeLocal(String value) {
  return DateTime.parse(value).toLocal();
}


class CustomDateTimeConverter implements JsonConverter<DateTime, String> {
  const CustomDateTimeConverter();

  @override
  DateTime fromJson(String json) {
    if (json.contains(".")) {
      json = json.substring(0, json.length - 1);
    }

    return DateTime.parse(json);
  }

  @override
  String toJson(DateTime json) => json.toIso8601String();
}
