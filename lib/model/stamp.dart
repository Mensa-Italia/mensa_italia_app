// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'stamp.freezed.dart';

part 'stamp.g.dart';

@freezed
abstract class StampModel with _$StampModel {
  const StampModel._();
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

  factory StampModel.fromJson(Map<String, dynamic> json) => _$StampModelFromJson(json);

  Uri getImageUri() {
    return Uri.parse(image);
  }

  Uri getThumbImageUri() {
    //set query parameters for thumb image thumb=0x50
    return Uri.parse(image).replace(queryParameters: {'thumb': '0x500'});
  }
}
