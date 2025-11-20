// ignore_for_file: invalid_annotation_target

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'deals_contact.freezed.dart';
part 'deals_contact.g.dart';

@freezed
abstract class DealsContact with _$DealsContact {
  const factory DealsContact({
    required String id,
    required String name,
    required String email,
    String? phoneNumber,
    String? note,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _DealsContact;

  factory DealsContact.fromJson(Map<String, dynamic> json) =>
      _$DealsContactFromJson(json);
}
