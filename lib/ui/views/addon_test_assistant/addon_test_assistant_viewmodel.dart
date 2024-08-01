import 'package:flutter/cupertino.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/testelab.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonTestAssistantViewModel extends MasterModel {
  int _page = 1;
  bool _isRequiringData = false;
  final List<TestelabModel> testelabs = [];
  double _scrollPosition = 0;
  String nameToSearch = "";
  TextEditingController searchController = TextEditingController();
  ScrollController scrollController = ScrollController();

  AddonTestAssistantViewModel() {
    scrollController.addListener(keepPagination);
    startRequestFlow();
  }

  startRequestFlow() {
    ScraperApi().getTestelab(page: _page, search: nameToSearch).then((value) {
      testelabs.clear();
      testelabs.addAll(value);
      rebuildUi();
    });
  }

  void keepPagination() {
    if (scrollController.position.pixels >= _scrollPosition + 100) {
      _scrollPosition = scrollController.position.maxScrollExtent;
      if (_isRequiringData) return;
      _isRequiringData = true;
      _page++;
      ScraperApi().getTestelab(page: _page, search: nameToSearch).then((value) {
        if (value.isEmpty) {
          _isRequiringData = true;
          return;
        }
        testelabs.addAll(value);
        rebuildUi();
        _isRequiringData = false;
      });
    }
  }

  Function() tapOnCandidate(int index) {
    return () {
      final candidate = testelabs[index];
      navigationService.navigateToGenericWebviewView(url: "https://www.cloud32.it/Associazioni/utenti/testelab/${candidate.id}/edit");
    };
  }

  void search(String value) {
    nameToSearch = value;
    _page = 1;
    startRequestFlow();
  }
}
