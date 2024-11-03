// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'event_schedule.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$EventScheduleModelImpl _$$EventScheduleModelImplFromJson(
        Map<String, dynamic> json) =>
    _$EventScheduleModelImpl(
      id: json['id'] as String?,
      title: json['title'] as String,
      event: json['event'] as String?,
      description: json['description'] as String,
      image: json['image'] as String?,
      whenStart: getDateTimeLocal(json['when_start'] as String),
      whenEnd: getDateTimeLocal(json['when_end'] as String),
      maxExternalGuests: (json['max_external_guests'] as num).toInt(),
      price: (json['price'] as num).toDouble(),
      infoLink: json['info_link'] as String,
      isSubscriptable: json['is_subscriptable'] as bool,
    );

Map<String, dynamic> _$$EventScheduleModelImplToJson(
        _$EventScheduleModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'title': instance.title,
      'event': instance.event,
      'description': instance.description,
      'image': instance.image,
      'when_start': instance.whenStart.toIso8601String(),
      'when_end': instance.whenEnd.toIso8601String(),
      'max_external_guests': instance.maxExternalGuests,
      'price': instance.price,
      'info_link': instance.infoLink,
      'is_subscriptable': instance.isSubscriptable,
    };
