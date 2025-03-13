// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'notification.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$NotificationModelImpl _$$NotificationModelImplFromJson(
        Map<String, dynamic> json) =>
    _$NotificationModelImpl(
      id: json['id'] as String,
      tr: json['tr'] as String,
      trNamedParams: Map<String, String>.from(json['tr_named_params'] as Map),
      data: json['data'] as Map<String, dynamic>,
      seen: getDateTimeLocalNullabe(json['seen'] as String),
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$$NotificationModelImplToJson(
        _$NotificationModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'tr': instance.tr,
      'tr_named_params': instance.trNamedParams,
      'data': instance.data,
      'seen': instance.seen?.toIso8601String(),
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
