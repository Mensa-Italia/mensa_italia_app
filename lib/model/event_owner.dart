import 'package:freezed_annotation/freezed_annotation.dart';

part 'event_owner.freezed.dart';
part 'event_owner.g.dart';

@freezed
class EventOwnerModel with _$EventOwnerModel {
  const factory EventOwnerModel({
    required String id,
    required String name,
    required String email,
    required String avatar,
  }) = _EventOwnerModel;

  factory EventOwnerModel.fromJson(Map<String, dynamic> json) =>
      _$EventOwnerModelFromJson(json);
}
