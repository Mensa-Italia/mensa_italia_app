// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'area_document.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_AreaDocumentModel _$AreaDocumentModelFromJson(Map<String, dynamic> json) =>
    _AreaDocumentModel(
      description: json['description'] as String,
      image: json['image'] as String,
      dimension: json['dimension'] as String,
      link: json['link'] as String,
    );

Map<String, dynamic> _$AreaDocumentModelToJson(_AreaDocumentModel instance) =>
    <String, dynamic>{
      'description': instance.description,
      'image': instance.image,
      'dimension': instance.dimension,
      'link': instance.link,
    };
