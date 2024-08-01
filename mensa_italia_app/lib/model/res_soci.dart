import 'package:freezed_annotation/freezed_annotation.dart';

part 'res_soci.freezed.dart';

part 'res_soci.g.dart';

@freezed
class RegSociModel with _$RegSociModel {
  const factory RegSociModel({
    required String id,
    required String image,
    required String name,
    required String city,
    required String state,
    required String linkToFullProfile,
  }) = _RegSociModel;

  factory RegSociModel.fromJson(Map<String, dynamic> json) =>
      _$RegSociModelFromJson(json);
}
