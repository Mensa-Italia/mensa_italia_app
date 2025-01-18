import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

class ChangelogModel extends MasterModel {
  void close() {
    navigationService.back();
  }
}
