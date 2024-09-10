import 'package:flutter/material.dart';
import 'package:isar/isar.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/database/database.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_sheet_regsoci/bottom_sheet_regsoci.dart';

class _AddonContactsUpdates {
  static bool started = false;
  static bool completed = false;

  static Future start() async {
    if (started) return;
    await (await ScraperApi().getCookieJar()).cookieJar.deleteAll();
    started = true;
    int threads = 10;
    await Future.wait(List.generate(
        threads, (index) => startRequestFlow(index + 1, window: threads)));
    completed = true;
  }

  static Future startRequestFlow(int page, {int window = 5}) {
    return ScraperApi().getRegSoci(page: page).then((value) async {
      try {
        DB.isar.writeTxn(() async {
          await DB.isar.regSociModels.putAll(value);
        });
      } catch (_) {}
      if (value.isNotEmpty) {
        await startRequestFlow(page + window, window: window);
      }
    });
  }
}

class AddonContactsViewModel extends MasterModel {
  final List<RegSociModel?> _contacts = [];
  String nameToSearch = "";
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();

  bool get isCompleted => _AddonContactsUpdates.completed;

  AddonContactsViewModel() {
    refresh();
    DB.isar.regSociModels.watchLazy().listen((event) {
      refresh();
    });
    _AddonContactsUpdates.start().then((value) {
      refresh();
    });
  }

  int get countMembers => _contacts.length;

  RegSociModel getElementAt(int index) {
    if (index < 0) {
      return _contacts[0]!;
    }
    return _contacts[index]!;
  }

  refresh() {
    _contacts.clear();
    if (nameToSearch.trim().isEmpty) {
      _contacts.addAll(DB.isar.regSociModels.buildQuery<RegSociModel>(
        sortBy: [
          const SortProperty(
            property: "name",
            sort: Sort.asc,
          ),
        ],
      ).findAllSync());
    } else {
      _contacts.addAll(DB.isar.regSociModels.buildQuery<RegSociModel>(
        filter: FilterGroup.or(nameToSearchCombination().map((e) {
          return FilterCondition.startsWith(
            property: 'nameFullTextSearch',
            value: e.trim(),
            caseSensitive: false,
          );
        }).toList()),
        sortBy: [
          const SortProperty(
            property: "name",
            sort: Sort.asc,
          ),
        ],
      ).findAllSync());
      if (_contacts.isEmpty) {
        _contacts.addAll(DB.isar.regSociModels.buildQuery<RegSociModel>(
          filter: FilterGroup.or(Isar.splitWords(nameToSearch).map((e) {
            return FilterCondition.startsWith(
              property: 'nameFullTextSearch',
              value: e.trim(),
              caseSensitive: false,
            );
          }).toList()),
          sortBy: [
            const SortProperty(
              property: "name",
              sort: Sort.asc,
            ),
          ],
        ).findAllSync());
      }
    }

    rebuildUi();
  }

  Function() tapOnContact(int index) {
    final contact = _contacts[index];
    return () {
      showBeautifulBottomSheet(
        child: BottomSheetRegsoci(
          regSoci: contact!,
        ),
      );
    };
  }

  void search(String value) {
    nameToSearch = value;
    refresh();
  }

  List<String> nameToSearchCombination() {
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
}
