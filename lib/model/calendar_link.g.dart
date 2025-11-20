// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'calendar_link.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_CalendarLinkModel _$CalendarLinkModelFromJson(Map<String, dynamic> json) =>
    _CalendarLinkModel(
      id: json['id'] as String,
      user: json['user'] as String,
      hash: json['hash'] as String,
      state: (json['state'] as List<dynamic>).map((e) => e as String).toList(),
    );

Map<String, dynamic> _$CalendarLinkModelToJson(_CalendarLinkModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'user': instance.user,
      'hash': instance.hash,
      'state': instance.state,
    };
