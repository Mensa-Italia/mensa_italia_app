// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';

import 'parser_tools.dart';

part 'notification.freezed.dart';

part 'notification.g.dart';

@freezed
abstract class NotificationModel with _$NotificationModel {
  const NotificationModel._();
  const factory NotificationModel({
    required String id,
    required String tr,
    required Map<String, String> trNamedParams,
     Map<String, dynamic>? data,
    @JsonKey(
      fromJson: getDateTimeLocalNullabe,
    )
    required DateTime? seen,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _NotificationModel;

  factory NotificationModel.fromJson(Map<String, dynamic> json) =>
      _$NotificationModelFromJson(json);

  String get title {
    return "$tr.title";
  }

  String get body {
    return "$tr.body";
  }
}
