import 'package:freezed_annotation/freezed_annotation.dart';
import 'package:isar/isar.dart';

part 'res_soci.freezed.dart';

part 'res_soci.g.dart';

@freezed
@Collection(ignore: {'copyWith'})
class RegSociModel with _$RegSociModel {
  const RegSociModel._();
  const factory RegSociModel({
    required int uid,
    required String image,
    required String name,
    required String city,
    required DateTime? birthDate,
    required String state,
    required String linkToFullProfile,
  }) = _RegSociModel;

  Id get id => (uid);

  factory RegSociModel.fromJson(Map<String, dynamic> json) =>
      _$RegSociModelFromJson(json);

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
