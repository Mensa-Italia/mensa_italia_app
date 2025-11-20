// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'document.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_DocumentModel _$DocumentModelFromJson(Map<String, dynamic> json) =>
    _DocumentModel(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String?,
      file: json['file'] as String,
      uploadedBy: json['uploaded_by'] as String,
      category: json['category'] as String,
      elaborated: json['elaborated'] as String,
    );

Map<String, dynamic> _$DocumentModelToJson(_DocumentModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'file': instance.file,
      'uploaded_by': instance.uploadedBy,
      'category': instance.category,
      'elaborated': instance.elaborated,
    };
