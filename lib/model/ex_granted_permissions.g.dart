// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ex_granted_permissions.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_ExGrantedPermissionsModel _$ExGrantedPermissionsModelFromJson(
        Map<String, dynamic> json) =>
    _ExGrantedPermissionsModel(
      id: json['id'] as String,
      user: json['user'] as String,
      exApp: json['ex_app'] as String,
      permissions: (json['permissions'] as List<dynamic>)
          .map((e) => e as String)
          .toList(),
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$ExGrantedPermissionsModelToJson(
        _ExGrantedPermissionsModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'user': instance.user,
      'ex_app': instance.exApp,
      'permissions': instance.permissions,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
