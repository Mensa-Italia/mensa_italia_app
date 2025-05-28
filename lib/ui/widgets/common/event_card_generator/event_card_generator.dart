import 'dart:ui';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:shimmer/shimmer.dart';
import 'package:stacked/stacked.dart';

import 'event_card_generator_model.dart';

class EventCardGenerator extends StackedView<EventCardGeneratorModel> {
  const EventCardGenerator({super.key});

  @override
  Widget builder(
    BuildContext context,
    EventCardGeneratorModel viewModel,
    Widget? child,
  ) {
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
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 0),
                Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: AspectRatio(
                    aspectRatio: 1600 / 900,
                    child: viewModel.isBusy
                        ? Shimmer.fromColors(
                            baseColor: Colors.grey[100]!,
                            highlightColor: Colors.grey[400]!,
                            child: Container(
                              decoration: BoxDecoration(
                                borderRadius: BorderRadius.circular(20),
                                color: Colors.grey[300],
                              ),
                            ),
                          )
                        : viewModel.generatedImage != null
                            ? Container(
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(20),
                                  image: DecorationImage(
                                    image: MemoryImage(viewModel.generatedImage!),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                              )
                            : CachedNetworkImage(
                                imageUrl: viewModel.imageUrl.toString(),
                                imageBuilder: (context, imageProvider) {
                                  return Container(
                                    decoration: BoxDecoration(
                                      borderRadius: BorderRadius.circular(20),
                                      image: DecorationImage(
                                        image: imageProvider,
                                        fit: BoxFit.cover,
                                      ),
                                    ),
                                  );
                                },
                                progressIndicatorBuilder: (context, url, downloadProgress) {
                                  return Shimmer.fromColors(
                                    baseColor: Colors.grey[100]!,
                                    highlightColor: Colors.grey[400]!,
                                    child: Container(
                                      decoration: BoxDecoration(
                                        borderRadius: BorderRadius.circular(20),
                                        color: Colors.grey[300],
                                      ),
                                    ),
                                  );
                                },
                                errorWidget: (context, url, error) {
                                  return Container(
                                    color: Colors.grey[300],
                                    child: const Center(child: Text("Immagine non disponibile")),
                                  );
                                }),
                  ),
                ),
                const SizedBox(height: 20),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 20),
                  child: Form(
                    key: viewModel.formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        TextFormField(
                          decoration: InputDecoration(
                            hintText: "TITOLO BREVE",
                          ),
                          textCapitalization: TextCapitalization.characters,
                          onSaved: viewModel.setShortTitle,
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          decoration: InputDecoration(
                            hintText: "LUNEDÌ 1 GENNAIO",
                          ),
                          textCapitalization: TextCapitalization.characters,
                          onSaved: viewModel.setDate,
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          decoration: InputDecoration(
                            hintText: "ORE 21:00",
                          ),
                          textCapitalization: TextCapitalization.characters,
                          onSaved: viewModel.setTime,
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          decoration: InputDecoration(
                            hintText: "RISTORANTE BELLISSIMO",
                          ),
                          textCapitalization: TextCapitalization.characters,
                          onSaved: viewModel.setRestaurantName,
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          decoration: InputDecoration(
                            hintText: "VIA ROMA 1",
                          ),
                          textCapitalization: TextCapitalization.characters,
                          onSaved: viewModel.setAddress,
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          decoration: InputDecoration(
                            hintText: "MILANO (MI)",
                          ),
                          textCapitalization: TextCapitalization.characters,
                          onSaved: viewModel.setCity,
                        ),
                        const SizedBox(height: 20),
                        if (!viewModel.isBusy)
                          ElevatedButton(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.black,
                              foregroundColor: Colors.white,
                            ),
                            onPressed: viewModel.generate,
                            child: const Text("GENERATE"),
                          ),
                        if (viewModel.imageUrl.toString() != "https://svc.mensa.it/static/event_card_template.png" && !viewModel.isBusy) ...[
                          const SizedBox(height: 20),
                          ElevatedButton(
                            onPressed: viewModel.sendBack,
                            child: const Text("PERFECT!"),
                          ),
                        ]
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 40),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  EventCardGeneratorModel viewModelBuilder(BuildContext context) => EventCardGeneratorModel();
}
