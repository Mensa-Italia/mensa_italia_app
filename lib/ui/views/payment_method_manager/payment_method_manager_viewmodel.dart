import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/payment_method.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class PaymentMethodManagerViewModel extends MasterModel {
  final controller = CardFormEditController();
  final List<InternalPaymentMethod> paymentMethods = [];
  dynamic customer;
  String selectedPM = "";
  PaymentMethodManagerViewModel() {
    load();
  }

  void load() {
    setBusy(true);
    Api().getCustomer().then((value) {
      customer = value;
      Api().getPaymentMethods().then((value) {
        paymentMethods.clear();
        paymentMethods.addAll(value);
        selectedPM = getSelectedPaymentMethod();
        if (selectedPM == "") {
          Api().setDefaultPaymentMethod(paymentMethods[0].id).then((_) {
            load();
          });
        } else {
          setBusy(false);
        }
      });
    });
  }

  String getSelectedPaymentMethod() {
    if (customer == null) return "";
    try {
      return customer["invoice_settings"]["default_payment_method"]["id"];
    } catch (_) {
      return "";
    }
  }

  void addPaymentMethod() {
    Api().newPaymentIntent().then((seti) async {
      Stripe.instance
          .initPaymentSheet(
        paymentSheetParameters: SetupPaymentSheetParameters(
          setupIntentClientSecret: seti["client_secret"],
          customerId: seti["customer"]["id"],
          returnURL: "mensa://stripe-redirect",
          allowsDelayedPaymentMethods: true,
          applePay: Stripe.merchantIdentifier == null
              ? null
              : const PaymentSheetApplePay(
                  merchantCountryCode: "IT",
                  buttonType: PlatformButtonType.setUp,
                ),
          googlePay: const PaymentSheetGooglePay(
            merchantCountryCode: "IT",
          ),
          merchantDisplayName: "Mensa Italia",
        ),
      )
          .then((_) {
        Stripe.instance.presentPaymentSheet().then((options) {
          load();
        });
      });
    });
  }

  void onPaymentMethodSelected(int? p1) {
    if (p1 == null) return;
    selectedPM = paymentMethods[p1].id;
    rebuildUi();
    Api().setDefaultPaymentMethod(paymentMethods[p1].id).then((_) {
      load();
    });
  }

  bool isSelected(InternalPaymentMethod paymentMethod) {
    return paymentMethod.id == selectedPM;
  }
}
