import 'package:freezed_annotation/freezed_annotation.dart';

part 'deals_contact.freezed.dart';
part 'deals_contact.g.dart';

@freezed
class DealsContact with _$DealsContact {
  const factory DealsContact({
    required String id,
    required String name,
    required String email,
    String? phoneNumber,
    String? note,
    required DateTime created,
    required DateTime updated,
  }) = _DealsContact;

  factory DealsContact.fromJson(Map<String, dynamic> json) =>
      _$DealsContactFromJson(json);
}
