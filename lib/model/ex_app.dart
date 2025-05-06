// ignore_for_file: invalid_annotation_target

/*
{
      "collectionId": "pbc_3118183718",
      "collectionName": "ex_apps",
      "id": "test",
      "name": "test",
      "description": "test",
      "image": "filename.jpg",
      "created": "2022-01-01 10:00:00.123Z",
      "updated": "2022-01-01 10:00:00.123Z"
    }
    */

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'ex_app.freezed.dart';
part 'ex_app.g.dart';

@freezed
class ExAppModel with _$ExAppModel {
  factory ExAppModel({
    String? collectionId,
    String? collectionName,
    String? id,
    String? name,
    String? description,
    String? image,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    DateTime? created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    DateTime? updated,
  }) = _ExAppModel;

  factory ExAppModel.fromJson(Map<String, dynamic> json) =>
      _$ExAppModelFromJson(json);
}
