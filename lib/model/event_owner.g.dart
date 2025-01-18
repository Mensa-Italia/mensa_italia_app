// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event_owner.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$EventOwnerModelImpl _$$EventOwnerModelImplFromJson(
        Map<String, dynamic> json) =>
    _$EventOwnerModelImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      email: json['email'] as String,
      avatar: json['avatar'] as String,
    );

Map<String, dynamic> _$$EventOwnerModelImplToJson(
        _$EventOwnerModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'email': instance.email,
      'avatar': instance.avatar,
    };
