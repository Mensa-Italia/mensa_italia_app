// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'sig.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$SigModelImpl _$$SigModelImplFromJson(Map<String, dynamic> json) =>
    _$SigModelImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      image: json['image'] as String,
      link: json['link'] as String,
    );

Map<String, dynamic> _$$SigModelImplToJson(_$SigModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'image': instance.image,
      'link': instance.link,
    };
