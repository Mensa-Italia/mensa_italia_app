// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'document_elaborated.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_DocumentElaboratedModel _$DocumentElaboratedModelFromJson(
        Map<String, dynamic> json) =>
    _DocumentElaboratedModel(
      id: json['id'] as String,
      document: json['document'] as String,
      iaResume: json['ia_resume'] as String,
    );

Map<String, dynamic> _$DocumentElaboratedModelToJson(
        _DocumentElaboratedModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'document': instance.document,
      'ia_resume': instance.iaResume,
    };
