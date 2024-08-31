import 'package:in_app_review/in_app_review.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:shared_preferences/shared_preferences.dart';

class HomeViewModel extends MasterModel {
  int currentIndex = 2;

  HomeViewModel() {
    reviewApp();
  }

  void reviewApp() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    int review = prefs.getInt('review') ?? 0;
    if (review == 3) {
      final InAppReview inAppReview = InAppReview.instance;
      if (await inAppReview.isAvailable()) {
        inAppReview.requestReview();
        await prefs.setInt('review', review + 1);
      }
    } else {
      await prefs.setInt('review', review + 1);
    }
  }

  void bottomBarTapped(int value) {
    currentIndex = value;
    rebuildUi();
  }
}
