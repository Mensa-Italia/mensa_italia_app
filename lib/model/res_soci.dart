import 'dart:convert';

import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:isar/isar.dart';
import 'package:mensa_italia_app/model/parser_tools.dart';

part 'res_soci.freezed.dart';
part 'res_soci.g.dart';

@freezed
class RegSociModel with _$RegSociModel {
  const RegSociModel._();
  const factory RegSociModel({
    required String id,
    required String image,
    required String name,
    required String city,
    @JsonKey(
      fromJson: getDateTimeLocalNullabe,
    )
    required DateTime? birthdate,
    required String state,
    required Map<String, dynamic> fullData,
    required String? fullProfileLink,
  }) = _RegSociModel;

  factory RegSociModel.fromJson(Map<String, dynamic> json) =>
      _$RegSociModelFromJson(json);

  RegSociDBModel toDBModel() {
    return RegSociDBModel(
      uid: int.parse(id),
      image: image,
      name: name,
      city: city,
      birthdate: birthdate,
      state: state,
      fullDataJson: jsonEncode(fullData),
      fullProfileLink: fullProfileLink,
    );
  }

  String get firstWords {
    final words = name.split(" ");
    if (words.length > 1) {
      String toOutput = "";
      for (int i = 0; i < words.length; i++) {
        toOutput += words[i][0].toUpperCase();
      }
      return toOutput;
    } else {
      return words[0];
    }
  }
}

@freezed
@Collection(ignore: {'copyWith'})
class RegSociDBModel with _$RegSociDBModel {
  const RegSociDBModel._();
  const factory RegSociDBModel({
    required int uid,
    required String image,
    required String name,
    required String city,
    @JsonKey(
      fromJson: getDateTimeLocalNullabe,
    )
    required DateTime? birthdate,
    required String state,
    required String fullDataJson,
    required String? fullProfileLink,
  }) = _RegSociDBModel;

  Id get id => (uid);

  factory RegSociDBModel.fromJson(Map<String, dynamic> json) =>
      _$RegSociDBModelFromJson(json);

  @Index(type: IndexType.value, caseSensitive: false)
  List<String> get nameFullTextSearch => nameToSearchCombination(name);

  List<String> nameToSearchCombination(String nameToSearch) {
    final listOfWords = Isar.splitWords(nameToSearch);
    final List<String> result = [];

    // Funzione ricorsiva per trovare tutte le combinazioni
    void generateCombinations(
        List<String> currentCombination, List<String> remainingWords) {
      if (remainingWords.isEmpty) {
        result.add(currentCombination.join(" "));
      } else {
        for (int i = 0; i < remainingWords.length; i++) {
          List<String> nextCombination = List.from(currentCombination)
            ..add(remainingWords[i]);
          List<String> nextRemaining = List.from(remainingWords)..removeAt(i);
          generateCombinations(nextCombination, nextRemaining);
        }
      }
    }

    // Avvio dell'algoritmo ricorsivo
    generateCombinations([], listOfWords);

    return result;
  }

  RegSociModel toModel() {
    return RegSociModel(
      id: uid.toString(),
      image: image,
      name: name,
      city: city,
      birthdate: birthdate,
      state: state,
      fullData: fullData(),
      fullProfileLink: fullProfileLink,
    );
  }

  Map<String, dynamic> fullData() {
    try {
      return jsonDecode(fullDataJson);
    } catch (e) {
      return {};
    }
  }
}
