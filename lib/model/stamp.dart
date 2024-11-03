import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'stamp.freezed.dart';

part 'stamp.g.dart';





@freezed
class StampModel with _$StampModel {
  const factory StampModel({
    required String id,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
    required String description,
    required String image,
  }) = _StampModel;

  factory StampModel.fromJson(Map<String, dynamic> json) =>
      _$StampModelFromJson(json);
}