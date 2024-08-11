import 'package:freezed_annotation/freezed_annotation.dart';

part 'event_schedule.freezed.dart';

part 'event_schedule.g.dart';

@freezed
class EventScheduleModel with _$EventScheduleModel {
  const factory EventScheduleModel({
    String? id,
    required String title,
    String? event,
    required String description,
    String? image,
    required DateTime whenStart,
    required DateTime whenEnd,
    required int maxExternalGuests,
    required double price,
    required String infoLink,
    required bool isSubscriptable,
  }) = _EventScheduleModel;

  factory EventScheduleModel.fromJson(Map<String, dynamic> json) =>
      _$EventScheduleModelFromJson(json);
}
