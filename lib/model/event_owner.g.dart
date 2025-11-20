// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event_owner.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_EventOwnerModel _$EventOwnerModelFromJson(Map<String, dynamic> json) =>
    _EventOwnerModel(
      id: json['id'] as String,
      name: json['name'] as String,
      email: json['email'] as String,
      avatar: json['avatar'] as String,
    );

Map<String, dynamic> _$EventOwnerModelToJson(_EventOwnerModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'email': instance.email,
      'avatar': instance.avatar,
    };
