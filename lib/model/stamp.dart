import 'package:freezed_annotation/freezed_annotation.dart';

part 'stamp.freezed.dart';

part 'stamp.g.dart';





@freezed
class StampModel with _$StampModel {
  const factory StampModel({
    required String id,
    required DateTime created,
    required DateTime updated,
    required String description,
    required String image,
  }) = _StampModel;

  factory StampModel.fromJson(Map<String, dynamic> json) =>
      _$StampModelFromJson(json);
}