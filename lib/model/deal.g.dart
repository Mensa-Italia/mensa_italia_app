// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'deal.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$DealModelImpl _$$DealModelImplFromJson(Map<String, dynamic> json) =>
    _$DealModelImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      commercialSector: json['commercial_sector'] as String,
      position: getDataFromExpanded(json, 'position') == null
          ? null
          : LocationModel.fromJson(
              getDataFromExpanded(json, 'position') as Map<String, dynamic>),
      isLocal: json['is_local'] as bool,
      details: json['details'] as String?,
      who: json['who'] as String?,
      starting: json['starting'] == null
          ? null
          : DateTime.parse(json['starting'] as String),
      ending: json['ending'] == null
          ? null
          : DateTime.parse(json['ending'] as String),
      howToGet: json['how_to_get'] as String?,
      link: json['link'] as String?,
      owner: json['owner'] as String?,
      attachment: json['attachment'] as String?,
      isActive: json['is_active'] as bool,
      vatNumber: json['vat_number'] as String?,
      created: DateTime.parse(json['created'] as String),
      updated: DateTime.parse(json['updated'] as String),
    );

Map<String, dynamic> _$$DealModelImplToJson(_$DealModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'commercial_sector': instance.commercialSector,
      'position': instance.position?.toJson(),
      'is_local': instance.isLocal,
      'details': instance.details,
      'who': instance.who,
      'starting': instance.starting?.toIso8601String(),
      'ending': instance.ending?.toIso8601String(),
      'how_to_get': instance.howToGet,
      'link': instance.link,
      'owner': instance.owner,
      'attachment': instance.attachment,
      'is_active': instance.isActive,
      'vat_number': instance.vatNumber,
      'created': instance.created.toIso8601String(),
      'updated': instance.updated.toIso8601String(),
    };
