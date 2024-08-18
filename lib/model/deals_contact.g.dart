// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'deals_contact.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$DealsContactImpl _$$DealsContactImplFromJson(Map<String, dynamic> json) =>
    _$DealsContactImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      email: json['email'] as String,
      phoneNumber: json['phone_number'] as String?,
      note: json['note'] as String?,
      created: DateTime.parse(json['created'] as String),
      updated: DateTime.parse(json['updated'] as String),
    );

Map<String, dynamic> _$$DealsContactImplToJson(_$DealsContactImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'email': instance.email,
      'phone_number': instance.phoneNumber,
      'note': instance.note,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
