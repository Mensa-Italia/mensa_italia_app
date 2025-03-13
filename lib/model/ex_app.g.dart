// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ex_app.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ExAppModelImpl _$$ExAppModelImplFromJson(Map<String, dynamic> json) =>
    _$ExAppModelImpl(
      collectionId: json['collection_id'] as String?,
      collectionName: json['collection_name'] as String?,
      id: json['id'] as String?,
      name: json['name'] as String?,
      description: json['description'] as String?,
      image: json['image'] as String?,
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$$ExAppModelImplToJson(_$ExAppModelImpl instance) =>
    <String, dynamic>{
      'collection_id': instance.collectionId,
      'collection_name': instance.collectionName,
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'image': instance.image,
      'created': instance.created?.toIso8601String(),
      'updated': instance.updated?.toIso8601String(),
    };
