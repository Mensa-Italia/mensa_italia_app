import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/area_document.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonAreaDocumentsViewModel extends MasterModel {
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();

  List<AreaDocumentModel> documents = [];
  double _scrollPosition = 0;
  String nameToSearch = "";
  int _page = 1;
  bool _isRequiringData = false;

  AddonAreaDocumentsViewModel() {
    scrollController.addListener(keepPagination);
    startRequestFlow();
  }

  startRequestFlow() {
    ScraperApi().getAreaDocument(page: _page, search: nameToSearch).then((value) {
      documents.clear();
      documents.addAll(value);
      rebuildUi();
    });
  }

  void keepPagination() {
    if (scrollController.position.pixels >= _scrollPosition + 100) {
      _scrollPosition = scrollController.position.maxScrollExtent;
      if (_isRequiringData) return;
      _isRequiringData = true;
      _page++;
      ScraperApi().getAreaDocument(page: _page, search: nameToSearch).then((value) {
        if (value.isEmpty) {
          _isRequiringData = true;
          return;
        }
        documents.addAll(value);
        rebuildUi();
        _isRequiringData = false;
      });
    }
  }

  Function() onTap(AreaDocumentModel document) {
    return () async {
      navigationService.navigateToGenericWebviewView(
        url: document.link,
        title: "Document",
        previousPageTitle: "Documents",
      );
    };
  }

  void search(String value) {
    nameToSearch = value;
    _page = 1;
    startRequestFlow();
  }
}
