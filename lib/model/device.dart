/*
{
  "collectionId": "jd6v6egqp096rer",
  "collectionName": "users_devices",
  "id": "test",
  "user": "RELATION_RECORD_ID",
  "device_name": "test",
  "firebase_id": "test",
  "created": "2022-01-01 10:00:00.123Z",
  "updated": "2022-01-01 10:00:00.123Z"
}
*/

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'device.freezed.dart';
part 'device.g.dart';

@freezed
abstract class DeviceModel with _$DeviceModel {
  const factory DeviceModel({
    required String id,
    required String user,
    required String deviceName,
    required String firebaseId,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _DeviceModel;

  factory DeviceModel.fromJson(Map<String, dynamic> json) =>
      _$DeviceModelFromJson(json);
}
