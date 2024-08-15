// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/event.dart';
import 'package:mensa_italia_app/model/location.dart';

part 'deal.freezed.dart';
part 'deal.g.dart';

@freezed
class DealModel with _$DealModel {
  const factory DealModel({
    required String id,
    required String name,
    required String commercialSector,
    @JsonKey(
      readValue: getDataFromExpanded,
    )
    required LocationModel? position,
    required bool isLocal,
    String? details,
    String? who,
    DateTime? starting,
    DateTime? ending,
    String? howToGet,
    String? link,
    String? owner,
    String? attachment,
    required bool isActive,
    String? vatNumber,
    required DateTime created,
    required DateTime updated,
  }) = _DealModel;

  factory DealModel.fromJson(Map<String, dynamic> json) =>
      _$DealModelFromJson(json);
}
