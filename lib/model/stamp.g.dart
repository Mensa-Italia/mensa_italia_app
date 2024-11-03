// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'stamp.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$StampModelImpl _$$StampModelImplFromJson(Map<String, dynamic> json) =>
    _$StampModelImpl(
      id: json['id'] as String,
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
      description: json['description'] as String,
      image: json['image'] as String,
    );

Map<String, dynamic> _$$StampModelImplToJson(_$StampModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
      'description': instance.description,
      'image': instance.image,
    };
