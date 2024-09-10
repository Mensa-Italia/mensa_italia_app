// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'addon.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$AddonModelImpl _$$AddonModelImplFromJson(Map<String, dynamic> json) =>
    _$AddonModelImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      icon: json['icon'] as String,
      version: json['version'] as String,
      url: json['url'] as String,
    );

Map<String, dynamic> _$$AddonModelImplToJson(_$AddonModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'icon': instance.icon,
      'version': instance.version,
      'url': instance.url,
    };
