import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/payment_method.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';

class PaymentMethodPickerModel extends MasterModel {
  dynamic customer;
  bool showPicker = false;
  List<InternalPaymentMethod> listOfMethods = [];
  PaymentMethodPickerModel() {
    setBusy(true);
    load().then((_) {
      setBusy(false);
    });
  }

  Future load() async {
    return Api().getCustomer().then((_customer) async {
      customer = _customer;
      return Api().getPaymentMethods().then((_listOfMethods) {
        listOfMethods = _listOfMethods;
        rebuildUi();
      });
    });
  }

  InternalPaymentMethod? getMyPaymentMethod() {
    if (customer == null || listOfMethods.isEmpty) return null;
    try {
      return listOfMethods.firstWhere((element) =>
          element.id ==
          customer["invoice_settings"]["default_payment_method"]["id"]);
    } catch (e) {
      return null;
    }
  }

  addPaymentMethod() {
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
        Stripe.instance.presentPaymentSheet().then((_) {
          Stripe.instance
              .retrieveSetupIntent(seti["client_secret"])
              .then((data) {
            Api().setDefaultPaymentMethod(data.paymentMethodId).then((_) {
              load();
            });
          });
        });
      });
    });
  }

  void changePaymentMethod() {
    showPicker = !showPicker;
    rebuildUi();
  }

  void changePaymentMethodExec(int? p1) {
    setBusy(true);
    final choosedMethod = listOfMethods[p1!];
    Api().setDefaultPaymentMethod(choosedMethod.id).then((_) {
      load().then((_) {
        showPicker = false;
        setBusy(false);
      });
    }).catchError((e) {
      setBusy(false);
    });
  }

  void confirmPaymentMethod() {
    navigationService.back(
      result: listOfMethods.firstWhere((element) =>
          element.id ==
          customer["invoice_settings"]["default_payment_method"]["id"]),
    );
  }
}
