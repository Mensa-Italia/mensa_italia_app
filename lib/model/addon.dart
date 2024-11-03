import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:flutter/foundation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'addon.freezed.dart';
part 'addon.g.dart';

@freezed
class AddonModel with _$AddonModel {
  const factory AddonModel({
    required String id,
    required String name,
    required String description,
    required String icon,
    required String version,
    required String url,
  }) = _AddonModel;

  factory AddonModel.fromJson(Map<String, dynamic> json) =>
      _$AddonModelFromJson(json);
}
