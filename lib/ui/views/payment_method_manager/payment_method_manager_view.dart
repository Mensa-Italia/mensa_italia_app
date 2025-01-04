import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_stripe/flutter_stripe.dart';
import 'package:mensa_italia_app/model/payment_method.dart';
import 'package:stacked/stacked.dart';

import 'payment_method_manager_viewmodel.dart';

class PaymentMethodManagerView extends StackedView<PaymentMethodManagerViewModel> {
  const PaymentMethodManagerView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    PaymentMethodManagerViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      appBar: CupertinoNavigationBar(
        previousPageTitle: "views.settings.title".tr(),
        middle: Text(
          "views.payment_method_manager.title".tr(),
          maxLines: 1,
        ),
        trailing: TextButton(
          onPressed: viewModel.addPaymentMethod,
          child: Text("Aggiungi"),
        ),
      ),
      body: ListView.separated(
        itemCount: viewModel.paymentMethods.length,
        itemBuilder: (context, index) {
          return PaymentCardTile(
            ipm: viewModel.paymentMethods[index],
            index: index,
            selectedIndex: viewModel.selectedIndex,
            key: ValueKey(viewModel.paymentMethods[index].id),
            onChanged: viewModel.onPaymentMethodSelected,
          );
        },
        separatorBuilder: (context, index) {
          return Divider(
            indent: 20,
            endIndent: 20,
          );
        },
      ),
    );
  }

  @override
  PaymentMethodManagerViewModel viewModelBuilder(BuildContext context) => PaymentMethodManagerViewModel();
}

class PaymentCardTile extends StatelessWidget {
  final int index;
  final int selectedIndex;
  final InternalPaymentMethod ipm;
  final void Function(int?) onChanged;

  const PaymentCardTile({super.key, required this.ipm, required this.index, required this.selectedIndex, required this.onChanged});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: 20,
        vertical: 5,
      ),
      child: Row(
        children: [
          Container(
            width: 50,
            height: 50,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(300),
              color: Theme.of(context).primaryColorLight,
              image: DecorationImage(
                image: AssetImage(ipm.image),
                fit: BoxFit.cover,
              ),
            ),
          ),
          SizedBox(
            width: 10,
          ),
          Expanded(
            child: Text.rich(
              TextSpan(
                children: [
                  TextSpan(
                    text: ipm.brand,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  if (ipm.display.isNotEmpty)
                    TextSpan(
                      text: "\n",
                    ),
                  if (ipm.display.isNotEmpty)
                    TextSpan(
                      text: ipm.display,
                    ),
                ],
              ),
            ),
          ),
          Radio(
            value: index,
            groupValue: selectedIndex,
            onChanged: onChanged,
          ),
        ],
      ),
    );
  }
}
