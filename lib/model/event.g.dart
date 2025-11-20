// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_EventModel _$EventModelFromJson(Map<String, dynamic> json) => _EventModel(
      id: json['id'] as String,
      name: json['name'] as String,
      image: json['image'] as String,
      description: json['description'] as String,
      infoLink: json['info_link'] as String,
      bookingLink: json['booking_link'] as String,
      whenStart: getDateTimeLocal(json['when_start'] as String),
      whenEnd: getDateTimeLocal(json['when_end'] as String),
      contact: json['contact'] as String,
      isNational: json['is_national'] as bool,
      isSpot: json['is_spot'] as bool,
      owner: json['owner'] as String,
      position: getDataFromExpanded(json, 'position') == null
          ? null
          : LocationModel.fromJson(
              getDataFromExpanded(json, 'position') as Map<String, dynamic>),
    );

Map<String, dynamic> _$EventModelToJson(_EventModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'image': instance.image,
      'description': instance.description,
      'info_link': instance.infoLink,
      'booking_link': instance.bookingLink,
      'when_start': instance.whenStart.toIso8601String(),
      'when_end': instance.whenEnd.toIso8601String(),
      'contact': instance.contact,
      'is_national': instance.isNational,
      'is_spot': instance.isSpot,
      'owner': instance.owner,
      'position': instance.position?.toJson(),
    };
