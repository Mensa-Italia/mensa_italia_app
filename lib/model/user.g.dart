// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_UserModel _$UserModelFromJson(Map<String, dynamic> json) => _UserModel(
      id: json['id'] as String,
      username: json['username'] as String,
      name: json['name'] as String,
      avatar: json['avatar'] as String,
      email: json['email'] as String,
      expireMembership: getDateTimeLocal(json['expire_membership'] as String),
      powers:
          (json['powers'] as List<dynamic>).map((e) => e as String).toList(),
      addons:
          (json['addons'] as List<dynamic>).map((e) => e as String).toList(),
      isMembershipActive: json['is_membership_active'] as bool,
    );

Map<String, dynamic> _$UserModelToJson(_UserModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'username': instance.username,
      'name': instance.name,
      'avatar': instance.avatar,
      'email': instance.email,
      'expire_membership': instance.expireMembership.toIso8601String(),
      'powers': instance.powers,
      'addons': instance.addons,
      'is_membership_active': instance.isMembershipActive,
    };
