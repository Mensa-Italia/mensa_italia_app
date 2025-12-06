import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/deal.dart';
import 'package:mensa_italia_app/model/deals_contact.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class AddonDealsDetailsViewModel extends MasterModel {
  @override
  String componentName = "views.addons.deals.details.title";
  final DealModel deal;
  DealsContact? dealsContact;
  AddonDealsDetailsViewModel({required this.deal}) {
    Api().getDealsContacts(deal.id).then((value) {
      if (value.isEmpty) return;
      dealsContact = value.first;
      rebuildUi();
    });
  }
}
