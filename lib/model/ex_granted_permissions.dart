
/*
{
      "collectionId": "pbc_3548904148",
      "collectionName": "ex_granted_permissions",
      "id": "[object Object]2",
      "user": "RELATION_RECORD_ID",
      "ex_app": "RELATION_RECORD_ID",
      "permissions": [
        "CHECK_USER_EXISTENCE"
      ],
      "created": "2022-01-01 10:00:00.123Z",
      "updated": "2022-01-01 10:00:00.123Z"
    }
*/


import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'ex_granted_permissions.freezed.dart';
part 'ex_granted_permissions.g.dart';

@freezed
class ExGrantedPermissionsModel with _$ExGrantedPermissionsModel {
  const ExGrantedPermissionsModel._();

  const factory ExGrantedPermissionsModel({
    required String id,
    required String user,
    required String exApp,
    required List<String> permissions,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _ExGrantedPermissionsModel;

  factory ExGrantedPermissionsModel.fromJson(Map<String, dynamic> json) =>
      _$ExGrantedPermissionsModelFromJson(json);
}