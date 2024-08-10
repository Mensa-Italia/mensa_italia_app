import 'package:freezed_annotation/freezed_annotation.dart';

part 'calendar_link.freezed.dart';
part 'calendar_link.g.dart';

@freezed
class CalendarLinkModel with _$CalendarLinkModel {
  factory CalendarLinkModel({
    required String id,
    required String user,
    required String hash,
    required List<String> state,
  }) = _CalendarLinkModel;

  factory CalendarLinkModel.fromJson(Map<String, dynamic> json) =>
      _$CalendarLinkModelFromJson(json);
}
