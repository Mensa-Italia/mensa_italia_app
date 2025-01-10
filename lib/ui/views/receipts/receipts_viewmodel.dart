import 'dart:convert';

import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/receipt.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

class ReceiptsViewModel extends MasterModel {
  List<ReceiptModel> receipts = [];
  ReceiptsViewModel() {
    Api().pb.collection('payments').subscribe('*', (e) {
      if (e.action == "create") {
        receipts.add(ReceiptModel.fromJson(e.record!.toJson()));
      } else if (e.action == "update") {
        var index =
            receipts.indexWhere((element) => element.id == e.record!.id);
        if (index != -1) {
          receipts[index] = ReceiptModel.fromJson(e.record!.toJson());
        } else {
          receipts.add(ReceiptModel.fromJson(e.record!.toJson()));
        }
      } else if (e.action == "delete") {
        receipts.removeWhere((element) => element.id == e.record!.id);
      }
      receipts.sort((a, b) => b.created.compareTo(a.created));
      rebuildUi();
    }).then((value) {
      Api().getPaymentsReceipt().then((value) {
        receipts = value;
        receipts.sort((a, b) => b.created.compareTo(a.created));
        rebuildUi();
      });
    });
  }

  @override
  void dispose() {
    Api().pb.collection('payments').unsubscribe();
    super.dispose();
  }

  Function() finishPayment(ReceiptModel receipt) {
    if (receipt.status == "requires_payment_method") {
      return () {
        newMethodRequired(receipt.id);
      };
    } else if (receipt.status == "requires_confirmation") {
      return () {
        requireConfirmation(receipt.id);
      };
    } else if (receipt.status == "requires_action") {
      return () {
        requireAction(receipt.id);
      };
    }

    return () {};
  }

  newMethodRequired(String id) {
    Api().getPaymentIntent(id).then((value) {
      showPickPaymentMethod(value["amount"]).then((value2) {
        if (value2 != null) {
          Stripe.instance.confirmPayment(
            paymentIntentClientSecret: value["client_secret"],
            data: PaymentMethodParams.cardFromMethodId(
              paymentMethodData:
                  PaymentMethodDataCardFromMethod(paymentMethodId: value2.id),
            ),
          );
        }
      });
    });
  }

  requireConfirmation(String id) {
    Api().getPaymentIntent(id).then((value) {
      Stripe.instance.confirmPayment(
        paymentIntentClientSecret: value["client_secret"],
      );
    });
  }

  requireAction(String id) {
    Api().getPaymentIntent(id).then((value) {
      Stripe.instance.handleNextAction(
        value["client_secret"],
        returnURL: "mensa://stripe-redirect",
      );
    });
  }
}
