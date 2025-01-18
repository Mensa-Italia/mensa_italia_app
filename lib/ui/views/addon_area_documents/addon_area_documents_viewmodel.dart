import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/document.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonAreaDocumentsViewModel extends MasterModel {
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();

  List<DocumentModel> originalDocuments = [];
  List<DocumentModel> documents = [];
  List<String> categories = [];
  List<String> selectedCategories = [];
  String nameToSearch = "";

  AddonAreaDocumentsViewModel() {
    Api().getDocuments().then((value) {
      documents.clear();
      documents.addAll(value);
      originalDocuments.clear();
      originalDocuments.addAll(value);
      categories = originalDocuments.map((e) => e.category).toSet().toList();
      categories.sort();
      rebuildUi();
    });
  }

  Function() onTap(DocumentModel document) {
    return () async {
      navigationService.navigateToDocumentViewerView(
        downlaodUrl: document.file,
        title: "Document",
        previousPageTitle: "Documents",
      );
    };
  }

  void search(String value) {
    nameToSearch = value;
    applyFilters();
  }

  IconData getIconBasedOnCategory(String category) {
    switch (category) {
      case "bilanci": // Bilanci
        return EneftyIcons.status_up_outline;
      case "elezioni": // Elezioni
        return EneftyIcons.crown_outline;
      case "eventi_progetti": // Eventi e progetti
        return EneftyIcons.calendar_outline;
      case "materiale_comunicazione": // Materiale per comunicazione
        return EneftyIcons.folder_outline;
      case "modulitstica_contratti": // Modulistica e contratti
        return EneftyIcons.paperclip_outline;
      case "news_pubblicazioni": // News e pubblicazioni
        return EneftyIcons.document_outline;
      case "normativa_interna": // Normativa interna
        return EneftyIcons.judge_outline;
      case "verbali_delibere": // Verbali e delibere
        return EneftyIcons.book_outline;
      case "tesoreria_contabilità": // Tesoreria e contabilità
        return EneftyIcons.trend_up_outline;
      default:
        return EneftyIcons.document_2_outline;
    }
  }

  bool isActived(int index) {
    return selectedCategories.contains(categories[index]);
  }

  void Function() selectChip(int index) {
    return () {
      if (selectedCategories.contains(categories[index])) {
        selectedCategories.remove(categories[index]);
      } else {
        selectedCategories.add(categories[index]);
      }
      applyFilters();
    };
  }

  applyFilters() {
    documents.clear();
    if (selectedCategories.isEmpty) {
      documents.addAll(originalDocuments.where((element) =>
          element.name.toLowerCase().contains(nameToSearch.toLowerCase())));
    } else {
      documents.addAll(originalDocuments.where((element) =>
          element.name.toLowerCase().contains(nameToSearch.toLowerCase()) &&
          selectedCategories.contains(element.category)));
    }
    rebuildUi();
  }
}
