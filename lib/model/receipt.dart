// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';

import 'parser_tools.dart';

part 'receipt.freezed.dart';

part 'receipt.g.dart';

@freezed
class ReceiptModel with _$ReceiptModel {
  const factory ReceiptModel({
    required String id,
    required String? description,
    required String user,
    required String stripeCode,
    required String status,
    required int amount,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _ReceiptModel;

  factory ReceiptModel.fromJson(Map<String, dynamic> json) =>
      _$ReceiptModelFromJson(json);
}
