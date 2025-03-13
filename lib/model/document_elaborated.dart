import 'package:freezed_annotation/freezed_annotation.dart';

part 'document_elaborated.freezed.dart';
part 'document_elaborated.g.dart';

@freezed
class DocumentElaboratedModel with _$DocumentElaboratedModel {
  const factory DocumentElaboratedModel({
    required String id,
    required String document,
    required String iaResume,
  }) = _DocumentElaboratedModel;

  factory DocumentElaboratedModel.fromJson(Map<String, dynamic> json) =>
      _$DocumentElaboratedModelFromJson(json);
}
