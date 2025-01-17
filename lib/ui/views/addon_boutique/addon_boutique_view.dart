import 'package:cached_network_image/cached_network_image.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/boutique.dart';
import 'package:mensa_italia_app/ui/common/app_bar.dart';
import 'package:mensa_italia_app/ui/common/ui_helpers.dart';
import 'package:stacked/stacked.dart';

import 'addon_boutique_viewmodel.dart';

class AddonBoutiqueView extends StackedView<AddonBoutiqueViewModel> {
  const AddonBoutiqueView({Key? key}) : super(key: key);

  @override
  Widget builder(BuildContext context, AddonBoutiqueViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: getAppBarPlatform(
        title: "views.addons.boutique.title".tr(),
        previousPageTitle: "views.back.button.generic".tr(),
        searchBarActions: SearchBarActions(
          onChanged: viewModel.search,
          controller: viewModel.searchController,
          onSubmitted: viewModel.search,
          hintText: "views.addons.search.textfield.hint".tr(),
        ),
      ),
      bottomNavigationBar: Container(
        padding: const EdgeInsets.all(20),
        child: ElevatedButton(
          onPressed: viewModel.orderNow,
          child: Text("views.addons.boutique.order_now".tr()),
        ),
      ),
      body: ListView.builder(
        itemCount: viewModel.boutiques.length,
        itemBuilder: (context, index) {
          return _BoutiqueTile(
            key: ValueKey(viewModel.boutiques[index].id),
            product: viewModel.boutiques[index],
            onTap: () {
              viewModel.openProduct(viewModel.boutiques[index]);
            },
          );
        },
      ),
    );
  }

  @override
  AddonBoutiqueViewModel viewModelBuilder(BuildContext context) => AddonBoutiqueViewModel();
}

class _BoutiqueTile extends StatelessWidget {
  final BoutiqueModel product;
  final Function()? onTap;
  const _BoutiqueTile({super.key, required this.product, this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Row(
              children: [
                Container(
                  width: 100,
                  height: 100,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(10),
                    image: DecorationImage(
                      image: CachedNetworkImageProvider(product.image[0]),
                      fit: BoxFit.cover,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        product.name,
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                        ),
                        maxLines: 3,
                      ),
                      Text(
                        priceFormat(product.amount),
                        style: const TextStyle(
                          fontSize: 18,
                        ),
                      ),
                      Text(
                        product.description,
                        style: const TextStyle(
                          fontSize: 12,
                        ),
                        maxLines: 2,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
