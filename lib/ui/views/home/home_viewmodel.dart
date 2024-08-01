import 'package:mensa_italia_app/ui/common/master_model.dart';

class HomeViewModel extends MasterModel {
  int currentIndex = 2;

  void bottomBarTapped(int value) {
    currentIndex = value;
    rebuildUi();
  }
}
