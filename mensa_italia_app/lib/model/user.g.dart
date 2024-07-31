// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$UserModelImpl _$$UserModelImplFromJson(Map<String, dynamic> json) =>
    _$UserModelImpl(
      id: json['id'] as String,
      username: json['username'] as String,
      name: json['name'] as String,
      avatar: json['avatar'] as String,
      email: json['email'] as String,
      expireMembership: DateTime.parse(json['expire_membership'] as String),
      powers:
          (json['powers'] as List<dynamic>).map((e) => e as String).toList(),
      addons:
          (json['addons'] as List<dynamic>).map((e) => e as String).toList(),
    );

Map<String, dynamic> _$$UserModelImplToJson(_$UserModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'username': instance.username,
      'name': instance.name,
      'avatar': instance.avatar,
      'email': instance.email,
      'expire_membership': instance.expireMembership.toIso8601String(),
      'powers': instance.powers,
      'addons': instance.addons,
    };
