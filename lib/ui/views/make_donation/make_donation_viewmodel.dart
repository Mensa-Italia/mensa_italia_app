import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

class MakeDonationViewModel extends MasterModel {
  int selectedDontaion = 50;
  TextEditingController otherAmountController = TextEditingController();

  updateSelectedDonation(int value) {
    selectedDontaion = value;
    rebuildUi();
  }

  updateOtherAmount(String value) {
    selectedDontaion = int.tryParse(value) ?? 0;
    otherAmountController.text = selectedDontaion.toString();
    rebuildUi();
  }

  doTheDonation() {
    showPickPaymentMethod(
      selectedDontaion * 100,
    ).then((value) {
      if (value == null) return;
      Api().doDonation(selectedDontaion * 100).then((value) {
        Stripe.instance
            .confirmPayment(paymentIntentClientSecret: value['client_secret'])
            .then((value) {
          navigationService.popUntil((route) => route.isFirst);
          navigationService.navigateTo(Routes.receiptsView);
        });
      });
    });
  }
}
