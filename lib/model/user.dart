import 'package:freezed_annotation/freezed_annotation.dart';

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
    required DateTime expireMembership,
    required List<String> powers,
    required List<String> addons,
    required bool isMembershipActive,
  }) = _UserModel;

  factory UserModel.fromJson(Map<String, dynamic> json) =>
      _$UserModelFromJson(json);
}
