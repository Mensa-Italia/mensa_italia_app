// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'stamp_user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_StampUserModel _$StampUserModelFromJson(Map<String, dynamic> json) =>
    _StampUserModel(
      id: json['id'] as String,
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
      stamp: StampModel.fromJson(
          getDataFromExpanded(json, 'stamp') as Map<String, dynamic>),
      user: json['user'] as String,
    );

Map<String, dynamic> _$StampUserModelToJson(_StampUserModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
      'stamp': instance.stamp.toJson(),
      'user': instance.user,
    };
