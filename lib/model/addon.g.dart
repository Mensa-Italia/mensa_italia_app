// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'addon.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_AddonModel _$AddonModelFromJson(Map<String, dynamic> json) => _AddonModel(
      id: json['id'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      icon: json['icon'] as String,
      version: json['version'] as String,
      url: json['url'] as String,
    );

Map<String, dynamic> _$AddonModelToJson(_AddonModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'icon': instance.icon,
      'version': instance.version,
      'url': instance.url,
    };
