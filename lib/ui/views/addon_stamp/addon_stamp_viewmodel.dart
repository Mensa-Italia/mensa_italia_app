import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/stamp_user.dart';
import 'package:stacked/stacked.dart';

class AddonStampViewModel extends BaseViewModel {
  final List<StampUserModel> stamps = [];

  AddonStampViewModel() {
    Api().getStamps().then((value) {
      stamps.clear();
      stamps.addAll(value);
      rebuildUi();
    });
  }

  addStamp() {
    Api().addStamp("OKPawwRo3O6u24JZHgcsGGrQyNIoua").then((value) {
      Api().getStamps().then((value) {
        stamps.clear();
        stamps.addAll(value);
        rebuildUi();
      });
    });
  }
}
