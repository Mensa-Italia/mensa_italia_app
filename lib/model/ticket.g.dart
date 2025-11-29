// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'ticket.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_TicketModel _$TicketModelFromJson(Map<String, dynamic> json) => _TicketModel(
      id: json['id'] as String,
      name: json['name'] as String?,
      description: json['description'] as String?,
      userId: json['user_id'] as String?,
      link: json['link'] as String?,
      qr: json['qr'] as String?,
      internalRefId: json['internal_ref_id'] as String?,
      customerData: json['customer_data'] as String?,
      deadline: getDateTimeLocalNullabe(json['deadline'] as String),
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$TicketModelToJson(_TicketModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'description': instance.description,
      'user_id': instance.userId,
      'link': instance.link,
      'qr': instance.qr,
      'internal_ref_id': instance.internalRefId,
      'customer_data': instance.customerData,
      'deadline': instance.deadline?.toIso8601String(),
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
