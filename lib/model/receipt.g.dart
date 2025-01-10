// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'receipt.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$ReceiptModelImpl _$$ReceiptModelImplFromJson(Map<String, dynamic> json) =>
    _$ReceiptModelImpl(
      id: json['id'] as String,
      description: json['description'] as String?,
      user: json['user'] as String,
      stripeCode: json['stripe_code'] as String,
      status: json['status'] as String,
      amount: (json['amount'] as num).toInt(),
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$$ReceiptModelImplToJson(_$ReceiptModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'description': instance.description,
      'user': instance.user,
      'stripe_code': instance.stripeCode,
      'status': instance.status,
      'amount': instance.amount,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
