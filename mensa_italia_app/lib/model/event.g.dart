// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$EventModelImpl _$$EventModelImplFromJson(Map<String, dynamic> json) =>
    _$EventModelImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      image: json['image'] as String,
      description: json['description'] as String,
      infoLink: json['info_link'] as String,
      bookingLink: json['booking_link'] as String,
      when: DateTime.parse(json['when'] as String),
      contact: json['contact'] as String,
    );

Map<String, dynamic> _$$EventModelImplToJson(_$EventModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'image': instance.image,
      'description': instance.description,
      'info_link': instance.infoLink,
      'booking_link': instance.bookingLink,
      'when': instance.when.toIso8601String(),
      'contact': instance.contact,
    };
