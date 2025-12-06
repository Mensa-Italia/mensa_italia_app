import 'package:flutter/cupertino.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonDealsViewModel extends MasterModel {
  @override
  String componentName = "addons.deals.title";
  ScrollController scrollController = ScrollController();
  List<DealModel> _originalDeals = [];
  List<DealModel> deals = [];

  load() {
    Api().getDeals().then((value) {
      _originalDeals = value;
      deals = value;
      rebuildUi();
    });
  }

  AddonDealsViewModel() {
    load();
  }

  TextEditingController controller = TextEditingController();
  onChanged(String p1) {
    if (p1.isEmpty) {
      deals = _originalDeals;
      rebuildUi();
      return;
    }
    deals = _originalDeals
        .where(
          (element) => element.name.toLowerCase().contains(p1.toLowerCase()) || (element.details ?? "").toLowerCase().contains(p1.toLowerCase()) || (element.howToGet ?? "").toLowerCase().contains(p1.toLowerCase()) || (element.commercialSector).toLowerCase().contains(p1.toLowerCase()),
        )
        .toList();
    rebuildUi();
  }

  onSubmitted(String p1) {
    onChanged(p1);
  }

  void Function() tapOnDeal(int index) {
    return () {
      navigationService.navigateToAddonDealsDetailsView(
        deal: deals[index],
        previousPageTitle: componentName,
      );
    };
  }

  void tapAddDeal() {
    navigationService
        .navigateToAddonDealsAddView(
      previousPageTitle: componentName,
    )
        .then((value) {
      load();
    });
  }

  onLongPress(int index) {
    return () {
      navigationService
          .navigateToAddonDealsAddView(
        deal: deals[index],
        previousPageTitle: componentName,
      )
          .then((value) {
        load();
      });
    };
  }
}
