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
      whenStart: DateTime.parse(json['when_start'] as String),
      whenEnd: DateTime.parse(json['when_end'] as String),
      contact: json['contact'] as String,
      isNational: json['is_national'] as bool,
      owner: json['owner'] as String,
      position: getDataFromExpanded(json, 'position') == null
          ? null
          : LocationModel.fromJson(
              getDataFromExpanded(json, 'position') as Map<String, dynamic>),
    );

Map<String, dynamic> _$$EventModelImplToJson(_$EventModelImpl instance) =>
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
      'owner': instance.owner,
      'position': instance.position?.toJson(),
    };
