// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'user.freezed.dart';

part 'user.g.dart';

@freezed
class UserModel with _$UserModel {
  const factory UserModel({
    required String id,
    required String username,
    required String name,
    required String avatar,
    required String email,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime expireMembership,
    required List<String> powers,
    required List<String> addons,
    required bool isMembershipActive,
  }) = _UserModel;

  factory UserModel.fromJson(Map<String, dynamic> json) =>
      _$UserModelFromJson(json);
}
