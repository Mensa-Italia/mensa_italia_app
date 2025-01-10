import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/payment_method.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class PaymentMethodManagerViewModel extends MasterModel {
  final controller = CardFormEditController();
  final List<InternalPaymentMethod> paymentMethods = [];
  dynamic customer;
  int selectedIndex = -1;
  PaymentMethodManagerViewModel() {
    load();
  }

  void load() {
    Api().getCustomer().then((value) {
      customer = value;
      Api().getPaymentMethods().then((value) {
        paymentMethods.clear();
        paymentMethods.addAll(value);
        rebuildUi();
        if (selectedIndex == -1) {
          selectedIndex = getSelectedPaymentMethod();
          rebuildUi();
        }
      });
    });
  }

  int getSelectedPaymentMethod() {
    if (customer == null) return -1;
    return paymentMethods.indexWhere((element) =>
        element.id ==
        customer["invoice_settings"]["default_payment_method"]["id"]);
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
        ),
      )
          .then((_) {
        Stripe.instance.presentPaymentSheet().then((_) {
          load();
        });
      });
    });
  }

  void onPaymentMethodSelected(int? p1) {
    if (p1 == null) return;
    selectedIndex = p1;
    rebuildUi();
    Api().setDefaultPaymentMethod(paymentMethods[p1!].id).then((_) {
      load();
    });
  }
}
