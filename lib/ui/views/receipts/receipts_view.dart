import 'package:easy_localization/easy_localization.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/receipt.dart';
import 'package:mensa_italia_app/ui/common/ui_helpers.dart';
import 'package:stacked/stacked.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'receipts_viewmodel.dart';

class ReceiptsView extends StackedView<ReceiptsViewModel> {
  const ReceiptsView({Key? key}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    ReceiptsViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      appBar: CupertinoNavigationBar(
        previousPageTitle: "views.settings.title".tr(),
        middle: Text(
          "views.recieipt.title".tr(),
          maxLines: 1,
        ),
      ),
      body: viewModel.receipts.isEmpty
          ? Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Center(
                  child: Padding(
                    padding: const EdgeInsets.all(20).copyWith(bottom: 0),
                    child: Text(
                      "views.recieipt.no_receipts".tr(),
                      textAlign: TextAlign.center,
                    ),
                  ),
                ),
              ],
            )
          : ListView.builder(
              itemCount: viewModel.receipts.length,
              itemBuilder: (context, index) {
                return _receiptTile(
                  key: ValueKey(viewModel.receipts[index].id),
                  receipt: viewModel.receipts[index],
                  onTap: viewModel.finishPayment(viewModel.receipts[index]),
                );
              },
            ),
    );
  }

  @override
  ReceiptsViewModel viewModelBuilder(BuildContext context) => ReceiptsViewModel();
}

class _receiptTile extends StatelessWidget {
  final ReceiptModel receipt;
  final Function() onTap;
  const _receiptTile({super.key, required this.receipt, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return ListTile(
      onTap: () {
        if (receipt.status == "succeeded") {
          Api().getReceiptUrl(receipt.id).then((value) {
            launchUrlString(value);
          });
        } else {
          onTap();
        }
      },
      title: Text(receipt.description == null || receipt.description!.isEmpty ? "Donation" : receipt.description!),
      subtitle: Text(paymentStatusText()),
      contentPadding: const EdgeInsets.symmetric(horizontal: 20),
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: Colors.grey[300]!,
          ),
        ),
        child: getIcon(),
      ),
      trailing: Text(
        priceFormat(receipt.amount),
        style: TextStyle(
          fontSize: 16,
        ),
      ),
    );
  }

  //canceled, processing, requires_action, requires_capture, requires_confirmation, requires_payment_method, succeeded
  Icon getIcon() {
    if (receipt.status == "succeeded") {
      return Icon(
        EneftyIcons.receipt_2_bold,
      );
    } else if (receipt.status == "requires_action") {
      return Icon(
        EneftyIcons.warning_2_bold,
        color: Colors.orange, // Indicates an action is needed
      );
    } else if (receipt.status == "requires_confirmation") {
      return Icon(
        EneftyIcons.clipboard_bold,
        color: Colors.blue, // Confirmation pending
      );
    } else if (receipt.status == "requires_payment_method") {
      return Icon(
        EneftyIcons.card_bold,
        color: Colors.purple, // Payment method required
      );
    } else if (receipt.status == "requires_capture") {
      return Icon(
        EneftyIcons.box_bold,
        color: Colors.teal, // Capture required
      );
    } else if (receipt.status == "processing") {
      return Icon(
        EneftyIcons.clock_bold,
        color: Colors.amber, // Processing
      );
    } else if (receipt.status == "canceled") {
      return Icon(
        EneftyIcons.close_bold,
        color: Colors.red, // Canceled
      );
    } else {
      return Icon(
        EneftyIcons.info_circle_bold,
        color: Colors.grey, // Unknown status
      );
    }
  }

  String paymentStatusText() {
    if (receipt.status == "requires_action") {
      return "Action needed";
    } else if (receipt.status == "requires_confirmation") {
      return "Confirmation pending";
    } else if (receipt.status == "requires_payment_method") {
      return "Payment method required";
    } else if (receipt.status == "requires_capture") {
      return "Capture required";
    } else if (receipt.status == "processing") {
      return "Processing";
    } else if (receipt.status == "canceled") {
      return "Canceled";
    } else {
      return receipt.id;
    }
  }
}
