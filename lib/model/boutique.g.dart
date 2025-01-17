// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'boutique.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$BoutiqueModelImpl _$$BoutiqueModelImplFromJson(Map<String, dynamic> json) =>
    _$BoutiqueModelImpl(
      id: json['id'] as String,
      uid: json['uid'] as String,
      name: json['name'] as String,
      description: json['description'] as String,
      image: (json['image'] as List<dynamic>).map((e) => e as String).toList(),
      amount: (json['amount'] as num).toInt(),
      alternativeOf: json['alternative_of'] as String,
      created: getDateTimeLocal(json['created'] as String),
      updated: getDateTimeLocal(json['updated'] as String),
    );

Map<String, dynamic> _$$BoutiqueModelImplToJson(_$BoutiqueModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'uid': instance.uid,
      'name': instance.name,
      'description': instance.description,
      'image': instance.image,
      'amount': instance.amount,
      'alternative_of': instance.alternativeOf,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
