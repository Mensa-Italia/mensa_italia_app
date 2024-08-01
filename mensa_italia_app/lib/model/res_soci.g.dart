// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'res_soci.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$RegSociModelImpl _$$RegSociModelImplFromJson(Map<String, dynamic> json) =>
    _$RegSociModelImpl(
      id: json['id'] as String,
      image: json['image'] as String,
      name: json['name'] as String,
      city: json['city'] as String,
      state: json['state'] as String,
      linkToFullProfile: json['link_to_full_profile'] as String,
    );

Map<String, dynamic> _$$RegSociModelImplToJson(_$RegSociModelImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'image': instance.image,
      'name': instance.name,
      'city': instance.city,
      'state': instance.state,
      'link_to_full_profile': instance.linkToFullProfile,
    };
