// ignore_for_file: invalid_annotation_target

/*
 {
      "collectionId": "pbc_3781977165",
      "collectionName": "boutique",
      "id": "test",
      "uid": "test",
      "name": "test",
      "description": "test",
      "image": [
        "filename.jpg"
      ],
      "amount": 123,
      "alternative_of": "RELATION_RECORD_ID",
      "created": "2022-01-01 10:00:00.123Z",
      "updated": "2022-01-01 10:00:00.123Z"
    },
*/
import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'boutique.freezed.dart';

part 'boutique.g.dart';

@freezed
class BoutiqueModel with _$BoutiqueModel {
  const BoutiqueModel._();
  factory BoutiqueModel({
    required String id,
    required String uid,
    required String name,
    required String description,
    required List<String> image,
    required int amount,
    required String alternativeOf,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime created,
    @JsonKey(
      fromJson: getDateTimeLocal,
    )
    required DateTime updated,
  }) = _BoutiqueModel;

  factory BoutiqueModel.fromJson(Map<String, dynamic> json) =>
      _$BoutiqueModelFromJson(json);
}
