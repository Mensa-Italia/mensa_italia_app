// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'stamp_user.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$StampUserModelImpl _$$StampUserModelImplFromJson(Map<String, dynamic> json) =>
    _$StampUserModelImpl(
      id: json['id'] as String,
      created: DateTime.parse(json['created'] as String),
      updated: DateTime.parse(json['updated'] as String),
      stamp: StampModel.fromJson(
          getDataFromExpanded(json, 'stamp') as Map<String, dynamic>),
      user: json['user'] as String,
    );

Map<String, dynamic> _$$StampUserModelImplToJson(
        _$StampUserModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
      'stamp': instance.stamp.toJson(),
      'user': instance.user,
    };
