import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:gyro_provider/gyro_provider.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'front_card_shine_model.dart';

class FrontCardShine extends StackedView<FrontCardShineModel> {
  const FrontCardShine({super.key});

  @override
  Widget builder(
      BuildContext context, FrontCardShineModel viewModel, Widget? child) {
    return GyroProvider(
      builder: (context, gyroscope, rotation) {
        viewModel.calculateRotation(rotation);
        return Container(
          decoration: BoxDecoration(
            color: kcPrimaryColor,
            borderRadius: BorderRadius.circular(20),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.2),
                blurRadius: 3,
                offset: const Offset(0, 3),
              ),
            ],
          ),
          clipBehavior: Clip.antiAlias,
          child: Stack(
            children: [
              Positioned.fill(
                child: Row(
                  children: [
                    const Expanded(child: SizedBox()),
                    Center(
                      child: ShaderMask(
                        shaderCallback: (bounds) {
                          return LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: const [
                              Colors.grey,
                              Colors.white,
                              Colors.grey,
                            ],
                            stops: [
                              (1 - viewModel.normalizedRotation) - 0.5,
                              (1 - viewModel.normalizedRotation),
                              (1 - viewModel.normalizedRotation) + 0.5,
                            ],
                          ).createShader(bounds);
                        },
                        child: LayoutBuilder(
                          builder: (BuildContext context,
                              BoxConstraints constraints) {
                            return SvgPicture.asset(
                              "assets/svg/icon_full.svg",
                              colorFilter: const ColorFilter.mode(
                                Colors.white,
                                BlendMode.srcIn,
                              ),
                              width: constraints.maxHeight * 1 / 3,
                              height: constraints.maxHeight * 1 / 3,
                              fit: BoxFit.fitWidth,
                              alignment: Alignment.center,
                            );
                          },
                        ),
                      ),
                    ),
                    Expanded(
                      child: Container(
                        margin: const EdgeInsets.only(right: 10, bottom: 10),
                        alignment: Alignment.bottomRight,
                        child: const Icon(Icons.chevron_right,
                            color: Colors.white),
                      ),
                    ),
                  ],
                ),
              ),
              Positioned.fill(
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      stops: [
                        (1 - viewModel.normalizedRotation) - 0.5,
                        (1 - viewModel.normalizedRotation),
                        (1 - viewModel.normalizedRotation) + 0.5,
                      ],
                      colors: [
                        Colors.transparent,
                        Colors.white.withOpacity(.1),
                        Colors.transparent,
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  FrontCardShineModel viewModelBuilder(BuildContext context) =>
      FrontCardShineModel();
}
