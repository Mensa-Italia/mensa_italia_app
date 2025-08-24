import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/database/database.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/objectbox.g.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_sheet_regsoci/bottom_sheet_regsoci.dart';

class AddonContactsViewModel extends MasterModel {
  final List<RegSociModel?> _contacts = [];
  String nameToSearch = "";
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();
  final regSociBox = DB.store.box<RegSociDBModel>();

  AddonContactsViewModel() {
    regSociBox.query().watch(triggerImmediately: true).listen((event) {
      refresh();
    });
    if (regSociBox.isEmpty()) {
      setBusy(true);
    }
    Api().getRegSoci().then((value) {
      void addToDb(Store store, List<RegSociModel> models) {
        final tempBox = store.box<RegSociDBModel>();
        tempBox.putMany(models.map((e) => e.toDBModel()).toList());

        List<int> idsNotInDB = [];
        final allInDb = tempBox.getAll();
        List<int> myModelIds = models.map((e) => e.toDBModel().uid).toList();
        for (var element in allInDb) {
          if (!myModelIds.contains(element.uid)) {
            idsNotInDB.add(element.uid);
          }
        }

        tempBox.removeMany(idsNotInDB);
      }

      DB.store.runInTransactionAsync(TxMode.write, addToDb, value).then((_) {
        setBusy(false);
      });
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
    final value = regSociBox.query(buildQuery()).order(RegSociDBModel_.name).build().find();
    _contacts.clear();
    _contacts.addAll(value.map((e) => e.toModel()).toList());
    rebuildUi();
  }

  Condition<RegSociDBModel>? buildQuery() {
    Condition<RegSociDBModel>? query;
    if (nameToSearch.isNotEmpty) {
      final allWords = nameToSearchCombination();
      query = RegSociDBModel_.nameToSearch.contains(nameToSearch, caseSensitive: false);

      for (var word in allWords) {
        query = query!.or(RegSociDBModel_.nameToSearch.contains(word, caseSensitive: false));
      }
    }
    return query;
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
    final listOfWords = nameToSearch.trim().split(" ");
    final List<String> result = [];

    // Funzione ricorsiva per trovare tutte le combinazioni
    void generateCombinations(List<String> currentCombination, List<String> remainingWords) {
      if (remainingWords.isEmpty) {
        result.add(currentCombination.join(" "));
      } else {
        for (int i = 0; i < remainingWords.length; i++) {
          List<String> nextCombination = List.from(currentCombination)..add(remainingWords[i]);
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
