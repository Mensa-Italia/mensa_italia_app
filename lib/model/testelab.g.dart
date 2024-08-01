// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'testelab.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$TestelabModelImpl _$$TestelabModelImplFromJson(Map<String, dynamic> json) =>
    _$TestelabModelImpl(
      id: json['id'] as String,
      fullname: json['fullname'] as String,
      typeOfTest: json['type_of_test'] as String,
      modality: json['modality'] as String,
      status: json['status'] as String,
      state: json['state'] as String,
    );

Map<String, dynamic> _$$TestelabModelImplToJson(_$TestelabModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'fullname': instance.fullname,
      'type_of_test': instance.typeOfTest,
      'modality': instance.modality,
      'status': instance.status,
      'state': instance.state,
    };
