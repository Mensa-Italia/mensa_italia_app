import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/receipt.dart';
import 'package:mensa_italia_app/services/receipt_see.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class ReceiptsViewModel extends MasterModel {
  List<ReceiptModel> get receipts => ReceiptSSE().receipts;

  ReceiptsViewModel() {
    ReceiptSSE().addListener(rebuildUi);
    ReceiptSSE().start();
  }

  @override
  void dispose() {
    ReceiptSSE().removeListener(rebuildUi);
    ReceiptSSE().stop();
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
              paymentMethodData: PaymentMethodDataCardFromMethod(paymentMethodId: value2.id),
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
