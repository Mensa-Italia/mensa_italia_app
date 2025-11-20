// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'device.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_DeviceModel _$DeviceModelFromJson(Map<String, dynamic> json) => _DeviceModel(
      id: json['id'] as String,
      user: json['user'] as String,
      deviceName: json['device_name'] as String,
      firebaseId: json['firebase_id'] as String,
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$DeviceModelToJson(_DeviceModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'user': instance.user,
      'device_name': instance.deviceName,
      'firebase_id': instance.firebaseId,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
