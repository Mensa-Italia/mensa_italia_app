import 'package:freezed_annotation/freezed_annotation.dart';

part 'area_document.freezed.dart';

part 'area_document.g.dart';

@freezed
class AreaDocumentModel with _$AreaDocumentModel {
  factory AreaDocumentModel({
    required String description,
    required String image,
    required String dimension,
    required String link,
  }) = _AreaDocumentModel;

  factory AreaDocumentModel.fromJson(Map<String, dynamic> json) =>
      _$AreaDocumentModelFromJson(json);
}
