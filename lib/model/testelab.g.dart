// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'testelab.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_TestelabModel _$TestelabModelFromJson(Map<String, dynamic> json) =>
    _TestelabModel(
      id: json['id'] as String,
      fullname: json['fullname'] as String,
      typeOfTest: json['type_of_test'] as String,
      modality: json['modality'] as String,
      status: json['status'] as String,
      state: json['state'] as String,
    );

Map<String, dynamic> _$TestelabModelToJson(_TestelabModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'fullname': instance.fullname,
      'type_of_test': instance.typeOfTest,
      'modality': instance.modality,
      'status': instance.status,
      'state': instance.state,
    };
