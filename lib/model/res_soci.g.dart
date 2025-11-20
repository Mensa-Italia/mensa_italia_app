// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'res_soci.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_RegSociModel _$RegSociModelFromJson(Map<String, dynamic> json) =>
    _RegSociModel(
      id: json['id'] as String,
      image: json['image'] as String,
      name: json['name'] as String,
      city: json['city'] as String,
      birthdate: getDateTimeLocalNullabe(json['birthdate'] as String),
      state: json['state'] as String,
      fullData: json['full_data'] as Map<String, dynamic>,
      fullProfileLink: json['full_profile_link'] as String?,
    );

Map<String, dynamic> _$RegSociModelToJson(_RegSociModel instance) =>
    <String, dynamic>{
      'id': instance.id,
      'image': instance.image,
      'name': instance.name,
      'city': instance.city,
      'birthdate': instance.birthdate?.toIso8601String(),
      'state': instance.state,
      'full_data': instance.fullData,
      'full_profile_link': instance.fullProfileLink,
    };

_RegSociDBModel _$RegSociDBModelFromJson(Map<String, dynamic> json) =>
    _RegSociDBModel(
      uid: (json['uid'] as num).toInt(),
      image: json['image'] as String,
      name: json['name'] as String,
      city: json['city'] as String,
      birthdate: getDateTimeLocalNullabe(json['birthdate'] as String),
      state: json['state'] as String,
      fullDataJson: json['full_data_json'] as String,
      fullProfileLink: json['full_profile_link'] as String?,
      nameToSearch: json['name_to_search'] as String,
    );

Map<String, dynamic> _$RegSociDBModelToJson(_RegSociDBModel instance) =>
    <String, dynamic>{
      'uid': instance.uid,
      'image': instance.image,
      'name': instance.name,
      'city': instance.city,
      'birthdate': instance.birthdate?.toIso8601String(),
      'state': instance.state,
      'full_data_json': instance.fullDataJson,
      'full_profile_link': instance.fullProfileLink,
      'name_to_search': instance.nameToSearch,
    };
