import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/scraperapi.dart';
import 'package:mensa_italia_app/model/res_soci.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_sheet_regsoci/bottom_sheet_regsoci.dart';

class AddonContactsViewModel extends MasterModel {
  List<RegSociModel> contacts = [];
  double _scrollPosition = 0;
  String nameToSearch = "";
  int _page = 1;
  bool _isRequiringData = false;
  ScrollController scrollController = ScrollController();
  TextEditingController searchController = TextEditingController();

  AddonContactsViewModel() {
    scrollController.addListener(keepPagination);
    startRequestFlow();
  }

  startRequestFlow() {
    ScraperApi().getRegSoci(page: _page, search: nameToSearch).then((value) {
      contacts.clear();
      contacts.addAll(value);
      rebuildUi();
    });
  }

  void keepPagination() {
    if (scrollController.position.pixels >= _scrollPosition + 100) {
      _scrollPosition = scrollController.position.maxScrollExtent;
      if (_isRequiringData) return;
      _isRequiringData = true;
      _page++;
      ScraperApi().getRegSoci(page: _page, search: nameToSearch).then((value) {
        if (value.isEmpty) {
          _isRequiringData = true;
          return;
        }
        contacts.addAll(value);
        rebuildUi();
        _isRequiringData = false;
      });
    }
  }

  Function() tapOnContact(int index) {
    final contact = contacts[index];
    return () {
      showBeautifulBottomSheet(
        child: BottomSheetRegsoci(
          regSoci: contact,
        ),
      );
    };
  }

  void search(String value) {
    nameToSearch = value;
    _page = 1;
    startRequestFlow();
  }
}
