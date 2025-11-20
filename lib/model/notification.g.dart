// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'notification.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_NotificationModel _$NotificationModelFromJson(Map<String, dynamic> json) =>
    _NotificationModel(
      id: json['id'] as String,
      tr: json['tr'] as String,
      trNamedParams: Map<String, String>.from(json['tr_named_params'] as Map),
      data: json['data'] as Map<String, dynamic>?,
      seen: getDateTimeLocalNullabe(json['seen'] as String),
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$NotificationModelToJson(_NotificationModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'tr': instance.tr,
      'tr_named_params': instance.trNamedParams,
      'data': instance.data,
      'seen': instance.seen?.toIso8601String(),
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
