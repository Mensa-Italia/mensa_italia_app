import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:mensa_italia_app/ui/common/ui_helpers.dart';
import 'package:mensa_italia_app/ui/views/payment_method_manager/payment_method_manager_view.dart';
import 'package:stacked/stacked.dart';

import 'payment_method_picker_model.dart';

class PaymentMethodPicker extends StackedView<PaymentMethodPickerModel> {
  final int amount;
  const PaymentMethodPicker({super.key, this.amount = 0});

  @override
  Widget builder(
      BuildContext context, PaymentMethodPickerModel viewModel, Widget? child) {
    final paymentMethod = viewModel.getMyPaymentMethod();
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(30),
          topRight: Radius.circular(30),
        ),
      ),
      child: SafeArea(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Container(
                padding: const EdgeInsets.all(20).copyWith(left: 20),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Text(
                      "Metodo di pagamento",
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 20,
                      ),
                    ),
                    Spacer(),
                    IconButton(
                      onPressed: () {
                        Navigator.of(context).pop();
                      },
                      icon: Icon(
                        EneftyIcons.close_outline,
                        color: kcPrimaryColor,
                      ),
                    ),
                  ],
                ),
              ),
              if (viewModel.isBusy) ...[
                Center(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      vertical: 40,
                    ),
                    child: LoadingAnimationWidget.progressiveDots(
                      color: kcPrimaryColor,
                      size: 50,
                    ),
                  ),
                ),
              ],
              if (!viewModel.isBusy) ...[
                if (paymentMethod == null || viewModel.showPicker)
                  GestureDetector(
                    onTap: viewModel.addPaymentMethod,
                    child: Container(
                      padding: const EdgeInsets.all(20),
                      margin: const EdgeInsets.all(20).copyWith(top: 0),
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(10),
                        color: Colors.grey[100],
                      ),
                      child: Row(
                        children: [
                          Text(
                            "Aggiungi metodo di pagamento",
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const Spacer(),
                          Icon(
                            EneftyIcons.add_circle_bold,
                            color: kcPrimaryColor,
                          ),
                        ],
                      ),
                    ),
                  ),
                if (paymentMethod != null && !viewModel.showPicker)
                  PaymentCardTile(
                    ipm: paymentMethod,
                    index: 0,
                    onChanged: (int? index) {},
                    selectedIndex: 0,
                    showRadio: false,
                  ),
                if (paymentMethod != null && viewModel.showPicker)
                  Column(
                    children: List.generate(
                      viewModel.listOfMethods.length,
                      (index) => PaymentCardTile(
                        ipm: viewModel.listOfMethods[index],
                        index: index,
                        onChanged: viewModel.changePaymentMethodExec,
                        selectedIndex: viewModel.listOfMethods[index].id ==
                                paymentMethod.id
                            ? index
                            : -1,
                      ),
                    ),
                  ),
                if (!viewModel.showPicker)
                  Padding(
                    padding: const EdgeInsets.all(20).copyWith(bottom: 0),
                    child: ElevatedButton(
                      onPressed: viewModel.confirmPaymentMethod,
                      child: Text("Paga ${priceFormat(amount)}"),
                    ),
                  ),
                if (viewModel.showPicker)
                  Padding(
                    padding: const EdgeInsets.all(20).copyWith(bottom: 0),
                    child: ElevatedButton(
                      onPressed: viewModel.changePaymentMethod,
                      child: Text("Indietro"),
                    ),
                  ),
                if (!viewModel.showPicker)
                  Row(
                    children: [
                      Spacer(),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 20),
                        child: TextButton(
                          style: TextButton.styleFrom(
                            visualDensity: VisualDensity.compact,
                          ),
                          onPressed: viewModel.changePaymentMethod,
                          child: Text(
                            "Cambia metodo di pagamento",
                            style: TextStyle(
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  @override
  PaymentMethodPickerModel viewModelBuilder(
    BuildContext context,
  ) =>
      PaymentMethodPickerModel();
}
