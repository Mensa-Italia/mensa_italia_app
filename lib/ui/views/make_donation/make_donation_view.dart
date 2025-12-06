import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/ui/common/app_colors.dart';
import 'package:stacked/stacked.dart';

import 'make_donation_viewmodel.dart';

class MakeDonationView extends StackedView<MakeDonationViewModel> {
  final String previousPageTitle;
  const MakeDonationView({Key? key, required this.previousPageTitle}) : super(key: key);

  @override
  Widget builder(
      BuildContext context, MakeDonationViewModel viewModel, Widget? child) {
    return Scaffold(
      appBar: CupertinoNavigationBar(
        previousPageTitle: previousPageTitle.tr(),
        middle: Text(
          viewModel.componentName.tr(),
          maxLines: 1,
        ),
      ),
      body: GestureDetector(
        onTap: () => FocusScope.of(context).unfocus(),
        child: ListView(
          children: [
            _tileDonation(
              donation: 10,
              selectedDontaion: viewModel.selectedDontaion,
              onTap: viewModel.updateSelectedDonation,
            ),
            _tileDonation(
              donation: 20,
              selectedDontaion: viewModel.selectedDontaion,
              onTap: viewModel.updateSelectedDonation,
            ),
            _tileDonation(
              donation: 50,
              selectedDontaion: viewModel.selectedDontaion,
              onTap: viewModel.updateSelectedDonation,
            ),
            _tileDonation(
              donation: 100,
              selectedDontaion: viewModel.selectedDontaion,
              onTap: viewModel.updateSelectedDonation,
            ),
            _tileDonation(
              donation: 200,
              selectedDontaion: viewModel.selectedDontaion,
              onTap: viewModel.updateSelectedDonation,
            ),
            SizedBox(
              height: 20,
            ),
            Padding(
              padding: const EdgeInsets.all(15.0).copyWith(bottom: 0),
              child: Text(
                "views.make_donation.other_amount".tr(),
                style: TextStyle(
                  color: kcPrimaryColor,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.start,
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(15),
              child: TextFormField(
                keyboardType: TextInputType.numberWithOptions(
                  signed: false,
                  decimal: false,
                ),
                style: TextStyle(
                  color: kcPrimaryColor,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
                decoration: InputDecoration(
                  contentPadding: const EdgeInsets.all(20),
                  suffixText: "€",
                  hintText: "views.make_donation.other_amount_hint".tr(),
                  hintStyle: TextStyle(
                    color: Colors.grey,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                  border: OutlineInputBorder(
                    borderSide: BorderSide.none,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  filled: true,
                  fillColor: Colors.white,
                ),
                onChanged: viewModel.updateOtherAmount,
              ),
            ),
            SizedBox(
              height: 20,
            ),
            Padding(
              padding: const EdgeInsets.all(15.0),
              child: ElevatedButton(
                onPressed: viewModel.doTheDonation,
                child: Text(
                  "views.make_donation.button.donate".tr(),
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  MakeDonationViewModel viewModelBuilder(BuildContext context) =>
      MakeDonationViewModel();
}

class _tileDonation extends StatelessWidget {
  final int donation;
  final int selectedDontaion;
  final Function(int value) onTap;
  const _tileDonation(
      {required this.donation,
      required this.selectedDontaion,
      required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        onTap(donation);
      },
      child: Container(
        margin: const EdgeInsets.all(10),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: donation == selectedDontaion ? kcPrimaryColor : Colors.white,
          borderRadius: BorderRadius.circular(10),
        ),
        alignment: Alignment.center,
        child: Text(
          "$donation €",
          style: TextStyle(
            color: donation == selectedDontaion ? Colors.white : Colors.black,
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
          textAlign: TextAlign.center,
        ),
      ),
    );
  }
}
