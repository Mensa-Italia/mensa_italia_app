// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/location.dart';

part 'event.freezed.dart';

part 'event.g.dart';

@freezed
class EventModel with _$EventModel {
  const factory EventModel({
    required String id,
    required String name,
    required String image,
    required String description,
    required String infoLink,
    required String bookingLink,
    required DateTime whenStart,
    required DateTime whenEnd,
    required String contact,
    required bool isNational,
    @JsonKey(
      readValue: getDataFromExpanded,
    )
    required LocationModel? position,
  }) = _EventModel;

  factory EventModel.fromJson(Map<String, dynamic> json) =>
      _$EventModelFromJson(json);
}

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
