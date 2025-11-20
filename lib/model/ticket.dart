import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'ticket.freezed.dart';
part 'ticket.g.dart';

@freezed
abstract class TicketModel with _$TicketModel {
  const factory TicketModel({
    required String id,
    String? name,
    String? description,
    @JsonKey(name: 'user_id') String? userId,
    String? link,
    String? qr,
    @JsonKey(name: 'internal_ref_id') String? internalRefId,
    @JsonKey(
      fromJson: getDateTimeLocalNullabe,
    )
    DateTime? deadline,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _TicketModel;

  factory TicketModel.fromJson(Map<String, dynamic> json) =>
      _$TicketModelFromJson(json);
}
