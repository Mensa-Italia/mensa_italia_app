// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'calendar_link.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$CalendarLinkModelImpl _$$CalendarLinkModelImplFromJson(
        Map<String, dynamic> json) =>
    _$CalendarLinkModelImpl(
      id: json['id'] as String,
      user: json['user'] as String,
      hash: json['hash'] as String,
      state: (json['state'] as List<dynamic>).map((e) => e as String).toList(),
    );

Map<String, dynamic> _$$CalendarLinkModelImplToJson(
        _$CalendarLinkModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'user': instance.user,
      'hash': instance.hash,
      'state': instance.state,
    };
