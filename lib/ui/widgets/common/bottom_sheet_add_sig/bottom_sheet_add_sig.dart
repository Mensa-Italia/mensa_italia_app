import 'dart:ui';

import 'package:dotted_border/dotted_border.dart';
import 'package:enefty_icons/enefty_icons.dart';
import 'package:flutter/material.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:stacked/stacked.dart';

import 'bottom_sheet_add_sig_model.dart';

class BottomSheetAddSig extends StackedView<BottomSheetAddSigModel> {
  final SigModel? sig;
  const BottomSheetAddSig({super.key, this.sig});

  @override
  Widget builder(
      BuildContext context, BottomSheetAddSigModel viewModel, Widget? child) {
    return ClipRRect(
      borderRadius: const BorderRadius.only(
        topLeft: Radius.circular(30),
        topRight: Radius.circular(30),
      ),
      child: BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
        child: Container(
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(.6),
            borderRadius: const BorderRadius.only(
              topLeft: Radius.circular(30),
              topRight: Radius.circular(30),
            ),
            border: Border.all(color: Colors.white.withOpacity(.4), width: 2),
          ),
          child: SafeArea(
            top: false,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox(height: 20),
                Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    if (sig != null)
                      IconButton(
                        icon: const Icon(
                          EneftyIcons.trash_outline,
                          color: Colors.transparent,
                        ),
                        onPressed: () {},
                      ),
                    Expanded(
                      child: Text(
                        sig == null ? 'Create a community' : 'Edit community',
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 20,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    if (sig != null)
                      IconButton(
                        icon: const Icon(EneftyIcons.trash_outline),
                        onPressed: viewModel.deleteSig,
                        color: Colors.red,
                      ),
                  ],
                ),
                GestureDetector(
                  onTap: viewModel.pickImage,
                  child: Padding(
                    padding: const EdgeInsets.all(20.0),
                    child: DottedBorder(
                      borderType: BorderType.RRect,
                      radius: const Radius.circular(30),
                      strokeWidth: 3,
                      dashPattern: const [3, 5],
                      child: ClipRRect(
                        borderRadius:
                            const BorderRadius.all(Radius.circular(30)),
                        child: AspectRatio(
                          aspectRatio: 1528 / 603,
                          child: Container(
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: Colors.grey[200],
                              image: viewModel.imageBytes != null
                                  ? DecorationImage(
                                      image: MemoryImage(viewModel.imageBytes!),
                                      fit: BoxFit.cover,
                                    )
                                  : sig?.image != null
                                      ? DecorationImage(
                                          image: NetworkImage(sig!.image),
                                          fit: BoxFit.cover,
                                        )
                                      : null,
                            ),
                            child: !(viewModel.imageBytes != null ||
                                    sig?.image != null)
                                ? const Text(
                                    'Add Image',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                    ),
                                  )
                                : null,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                Form(
                  key: viewModel.formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: TextFormField(
                          controller: viewModel.nameController,
                          textInputAction: TextInputAction.next,
                          decoration: const InputDecoration(
                            hintText: 'Name',
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Please enter a name';
                            }
                            return null;
                          },
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: TextFormField(
                          controller: viewModel.linkController,
                          textInputAction: TextInputAction.done,
                          decoration: const InputDecoration(
                            hintText: 'Link',
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Please enter a link';
                            }
                            return null;
                          },
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: TextFormField(
                          controller: viewModel.sigTypeController,
                          textInputAction: TextInputAction.done,
                          decoration: const InputDecoration(
                            hintText: 'Type',
                          ),
                          onTap: viewModel.onTapSigType,
                          canRequestFocus: false,
                          enableInteractiveSelection: false,
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Please enter a type';
                            }
                            return null;
                          },
                        ),
                      ),
                      Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: ElevatedButton(
                          onPressed: viewModel.addSig,
                          child: viewModel.isBusy
                              ? LoadingAnimationWidget.beat(
                                  color: Colors.white.withOpacity(.8),
                                  size: 20,
                                )
                              : Text(sig == null ? 'CREATE' : 'UPDATE'),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  BottomSheetAddSigModel viewModelBuilder(BuildContext context) =>
      BottomSheetAddSigModel(sig: sig);
}
