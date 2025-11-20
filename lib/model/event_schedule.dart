// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'event_schedule.freezed.dart';

part 'event_schedule.g.dart';

@freezed
abstract class EventScheduleModel with _$EventScheduleModel {
  const factory EventScheduleModel({
    String? id,
    required String title,
    String? event,
    required String description,
    String? image,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime whenStart,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime whenEnd,
    required int maxExternalGuests,
    required double price,
    required String infoLink,
    required bool isSubscriptable,
  }) = _EventScheduleModel;

  factory EventScheduleModel.fromJson(Map<String, dynamic> json) =>
      _$EventScheduleModelFromJson(json);
}
