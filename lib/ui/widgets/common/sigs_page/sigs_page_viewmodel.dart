import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:url_launcher/url_launcher_string.dart';

class SigsPageModel extends MasterModel {
  final List<SigModel> _originalSigs = [];
  final List<SigModel> sigs = [];

  ScrollController scrollController = ScrollController();

  TextEditingController searchController = TextEditingController();

  SigsPageModel() {
    Api().getSigs().then((value) {
      _originalSigs.clear();
      _originalSigs.addAll(value);
      sigs.clear();
      sigs.addAll(value);
      rebuildUi();
    });
  }

  void search(String value) {
    if (value.isEmpty) {
      sigs.clear();
      sigs.addAll(_originalSigs);
    } else {
      sigs.clear();
      sigs.addAll(_originalSigs.where((element) =>
          element.name.toLowerCase().contains(value.toLowerCase().trim())));
    }
    rebuildUi();
  }

  Function() onTapOnSIG(SigModel sig) {
    return () async {
      if (await canLaunchUrlString(sig.link)) {
        launchUrlString(
          sig.link,
          mode: LaunchMode.externalApplication,
        );
      }
    };
  }
}
