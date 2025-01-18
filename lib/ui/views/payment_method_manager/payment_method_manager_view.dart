import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/model/payment_method.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'payment_method_manager_viewmodel.dart';

class PaymentMethodManagerView
    extends StackedView<PaymentMethodManagerViewModel> {
  const PaymentMethodManagerView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    PaymentMethodManagerViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      appBar: CupertinoNavigationBar(
        middle: Text(
          "views.payment_method_manager.title".tr(),
          maxLines: 1,
        ),
        trailing: IconButton(
          onPressed: viewModel.addPaymentMethod,
          icon: Icon(
            CupertinoIcons.add_circled_solid,
            size: 28,
            color: kcPrimaryColor,
          ),
        ),
      ),
      body: viewModel.isBusy && viewModel.paymentMethods.isEmpty
          ? Center(
              child: LoadingAnimationWidget.beat(
                color: kcPrimaryColor,
                size: 30,
              ),
            )
          : viewModel.paymentMethods.isEmpty
              ? Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Padding(
                      padding: const EdgeInsets.all(20).copyWith(bottom: 0),
                      child: Text(
                        "views.payment_method_manager.no_payment_methods".tr(),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    Center(
                      child: ElevatedButton(
                        onPressed: viewModel.addPaymentMethod,
                        style: ElevatedButton.styleFrom(
                          padding:
                              EdgeInsets.symmetric(vertical: 5, horizontal: 10),
                          minimumSize: Size(0, 0),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(300),
                          ),
                        ),
                        child: Text(
                            "views.payment_method_manager.add_payment_method"
                                .tr()),
                      ),
                    ),
                  ],
                )
              : ListView.separated(
                  itemCount: viewModel.paymentMethods.length,
                  itemBuilder: (context, index) {
                    return PaymentCardTile(
                      ipm: viewModel.paymentMethods[index],
                      index: index,
                      isSelected:
                          viewModel.isSelected(viewModel.paymentMethods[index]),
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
  PaymentMethodManagerViewModel viewModelBuilder(BuildContext context) =>
      PaymentMethodManagerViewModel();
}

class PaymentCardTile extends StatelessWidget {
  final int index;
  final bool isSelected;
  final InternalPaymentMethod ipm;
  final bool showRadio;
  final void Function(int?) onChanged;

  const PaymentCardTile({
    super.key,
    required this.ipm,
    required this.index,
    required this.isSelected,
    required this.onChanged,
    this.showRadio = true,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        onChanged(index);
      },
      child: Container(
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
            if (showRadio)
              Radio(
                value: index,
                groupValue: isSelected ? index : null,
                onChanged: onChanged,
              ),
          ],
        ),
      ),
    );
  }
}
