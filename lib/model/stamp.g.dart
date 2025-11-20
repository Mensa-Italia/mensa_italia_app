// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'stamp.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_StampModel _$StampModelFromJson(Map<String, dynamic> json) => _StampModel(
      id: json['id'] as String,
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
      description: json['description'] as String,
      image: json['image'] as String,
    );

Map<String, dynamic> _$StampModelToJson(_StampModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
      'description': instance.description,
      'image': instance.image,
    };
