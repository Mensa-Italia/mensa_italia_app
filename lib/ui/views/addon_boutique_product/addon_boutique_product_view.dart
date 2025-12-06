import 'package:cached_network_image/cached_network_image.dart';
import 'package:carousel_slider/carousel_slider.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/boutique.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:stacked/stacked.dart';

import 'addon_boutique_product_viewmodel.dart';

class AddonBoutiqueProductView extends StackedView<AddonBoutiqueProductViewModel> {
  final BoutiqueModel product;
  final String previousPageTitle;
  const AddonBoutiqueProductView({Key? key, required this.product, required this.previousPageTitle}) : super(key: key);

  @override
  Widget builder(
    BuildContext context,
    AddonBoutiqueProductViewModel viewModel,
    Widget? child,
  ) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: viewModel.componentName.tr(),
        previousPageTitle: previousPageTitle.tr(),
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(20),
        child: ElevatedButton(
          onPressed: viewModel.orderNow,
          child: Text("views.addons.boutique.order_now".tr()),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(0),
        children: [
          CarouselSlider.builder(
            options: CarouselOptions(
              height: 400.0,
              enableInfiniteScroll: false,
              aspectRatio: 1,
              viewportFraction: 1,
              onPageChanged: viewModel.onPageChanged,
            ),
            itemCount: product.image.length,
            itemBuilder: (context, index, realIndex) {
              return CachedNetworkImage(
                imageUrl: product.image[index],
                fit: BoxFit.cover,
              );
            },
          ),
          ValueListenableBuilder<int>(
            valueListenable: viewModel.currentPage,
            builder: (context, value, child) {
              return Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: product.image.asMap().entries.map((entry) {
                  return Container(
                    width: 8.0,
                    height: 8.0,
                    margin: const EdgeInsets.symmetric(vertical: 10.0, horizontal: 2.0),
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: (value == entry.key) ? const Color.fromRGBO(0, 0, 0, 0.9) : const Color.fromRGBO(0, 0, 0, 0.4),
                    ),
                  );
                }).toList(),
              );
            },
          ),
          Padding(
            padding: const EdgeInsets.all(10),
            child: Text(
              product.name,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(10),
            child: Text(
              product.description,
              style: const TextStyle(
                fontSize: 16,
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  AddonBoutiqueProductViewModel viewModelBuilder(BuildContext context) => AddonBoutiqueProductViewModel();
}
