import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

class SigsListViewModel extends MasterModel {
  final List<SigModel> sigs = [];

  SigsListViewModel() {
    Api().getSigs().then((value) {
      sigs.clear();
      sigs.addAll(value);
      rebuildUi();
    });
  }
}
