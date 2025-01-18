import 'package:mensa_italia_app/ui/common/master_model.dart';

class ChangelogModel extends MasterModel {
  void close() {
    navigationService.back();
  }
}
