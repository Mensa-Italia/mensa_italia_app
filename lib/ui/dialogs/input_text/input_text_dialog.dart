import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/ui_helpers.dart';
import 'package:stacked/stacked.dart';
import 'package:stacked_services/stacked_services.dart';

import 'input_text_dialog_model.dart';

const double _graphicSize = 60;

class InputTextDialog extends StackedView<InputTextDialogModel> {
  final DialogRequest request;
  final Function(DialogResponse) completer;

  const InputTextDialog({
    super.key,
    required this.request,
    required this.completer,
  });

  @override
  Widget builder(
      BuildContext context, InputTextDialogModel viewModel, Widget? child) {
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  request.title ?? '',
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
                verticalSpaceSmall,
                Text(
                  request.description ?? '',
                  style: const TextStyle(
                    fontSize: 16,
                  ),
                  textAlign: TextAlign.center,
                ),
                verticalSpaceSmall,
                verticalSpaceSmall,
                verticalSpaceSmall,
                TextFormField(
                  controller: viewModel.textController,
                ),
                verticalSpaceSmall,
                verticalSpaceSmall,
              ],
            ),
          ),
          ElevatedButton(
            onPressed: () => completer(DialogResponse<String>(
              confirmed: true,
              data: viewModel.textController.text,
            )),
            style: ElevatedButton.styleFrom(
              visualDensity: VisualDensity.compact,
              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              padding: const EdgeInsets.symmetric(vertical: 18),
              shape: const RoundedRectangleBorder(
                borderRadius: BorderRadius.only(
                  bottomLeft: Radius.circular(10),
                  bottomRight: Radius.circular(10),
                ),
              ),
            ),
            child: const Text(
              'OKAY',
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  InputTextDialogModel viewModelBuilder(BuildContext context) =>
      InputTextDialogModel(request: request);
}
