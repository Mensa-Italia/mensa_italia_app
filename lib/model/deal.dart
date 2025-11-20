// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/location.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'deal.freezed.dart';
part 'deal.g.dart';

@freezed
abstract class DealModel with _$DealModel {
  const DealModel._();

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
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    DateTime? starting,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    DateTime? ending,
    String? howToGet,
    String? link,
    String? owner,
    String? attachment,
    required bool isActive,
    String? vatNumber,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _DealModel;

  factory DealModel.fromJson(Map<String, dynamic> json) =>
      _$DealModelFromJson(json);

  String getWho() {
    if (who == null) {
      return "";
    }
    if (who == "active_members") {
      return "Active members";
    } else if (who == "active_members and relatives") {
      return "Active members and relatives";
    }
    return "";
  }
}
