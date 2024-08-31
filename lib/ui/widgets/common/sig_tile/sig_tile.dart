import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/sig.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'sig_tile_model.dart';

class SigTile extends StackedView<SigTileModel> {
  final SigModel sig;
  final void Function() onTap;
  final Function? onLongTap;
  const SigTile({super.key, required this.sig, required this.onTap, this.onLongTap});

  @override
  Widget builder(BuildContext context, SigTileModel viewModel, Widget? child) {
    return GestureDetector(
      onTap: onTap,
      onLongPress: () {
        if (onLongTap == null) return;
        viewModel.setBusy(false);
        onLongTap!();
      },
      onTapDown: (details) {
        if (onLongTap == null) return;
        viewModel.setBusy(true);
      },
      onTapUp: (details) {
        if (onLongTap == null) return;
        viewModel.setBusy(false);
      },
      onLongPressEnd: (details) {
        if (onLongTap == null) return;
        viewModel.setBusy(false);
      },
      child: AnimatedScale(
        scale: viewModel.isBusy ? 0.95 : 1,
        duration: const Duration(milliseconds: 300),
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: AspectRatio(
            aspectRatio: 1528 / 603,
            child: Container(
              clipBehavior: Clip.antiAlias,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(30),
                gradient: const LinearGradient(
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                  colors: [
                    kcPrimaryColor,
                    kcPrimaryColorDark,
                  ],
                ),
              ),
              child: sig.image.isEmpty
                  ? Center(
                      child: Text(
                        sig.name,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    )
                  : CachedNetworkImage(
                      imageUrl: sig.image,
                      fit: BoxFit.cover,
                      maxWidthDiskCache: 1131,
                      maxHeightDiskCache: 446,
                      memCacheWidth: 1131,
                      memCacheHeight: 446,
                    ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  SigTileModel viewModelBuilder(BuildContext context) => SigTileModel();
}
