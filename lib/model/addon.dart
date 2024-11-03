// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:flutter/foundation.dart';

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

  factory AddonModel.fromJson(Map<String, dynamic> json) => _$AddonModelFromJson(json);
}
