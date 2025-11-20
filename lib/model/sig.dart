import 'package:freezed_annotation/freezed_annotation.dart';

part 'sig.freezed.dart';
part 'sig.g.dart';

@freezed
abstract class SigModel with _$SigModel {
  const factory SigModel({
    required String id,
    required String name,
    required String description,
    required String image,
    required String link,
    required String groupType,
  }) = _SigModel;

  factory SigModel.fromJson(Map<String, dynamic> json) =>
      _$SigModelFromJson(json);
}
