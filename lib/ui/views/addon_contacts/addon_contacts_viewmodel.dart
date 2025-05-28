import 'package:flutter/material.dart';
import 'package:isar/isar.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/database/database.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_sheet_regsoci/bottom_sheet_regsoci.dart';

class AddonContactsViewModel extends MasterModel {
  final List<RegSociModel?> _contacts = [];
  String nameToSearch = "";
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();

  AddonContactsViewModel() {
    refresh();
    DB.isar.regSociDBModels.watchLazy().listen((event) {
      refresh();
    });
    Api().getRegSoci().then((value) {
      if (value != null) {
        DB.isar.writeTxnSync(() {
          for (var element in value) {
            DB.isar.regSociDBModels.putSync(element.toDBModel());
          }
          // get all the contacts in the database
          List<int> idsNotInDB = [];
          DB.isar.regSociDBModels.where().findAllSync().forEach((element) {
            if (!value.map((e) => e.id).contains(element.id.toString())) {
              idsNotInDB.add(element.id);
            }
          });
          // delete the contacts not in the database
          DB.isar.regSociDBModels.deleteAllSync(idsNotInDB);
        });
      }
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
      _contacts.addAll(DB.isar.regSociDBModels
          .buildQuery<RegSociDBModel>(
            sortBy: [
              const SortProperty(
                property: "name",
                sort: Sort.asc,
              ),
            ],
          )
          .findAllSync()
          .map((e) => e.toModel())
          .toList());
    } else {
      _contacts.addAll(DB.isar.regSociDBModels
          .buildQuery<RegSociDBModel>(
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
          )
          .findAllSync()
          .map((e) => e.toModel())
          .toList());
      if (_contacts.isEmpty) {
        _contacts.addAll(DB.isar.regSociDBModels
            .buildQuery<RegSociDBModel>(
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
            )
            .findAllSync()
            .map((e) => e.toModel())
            .toList());
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
