import 'dart:io';
import 'dart:ui';

import 'package:auto_size_text/auto_size_text.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:flutter/material.dart';
import 'package:mensa_italia_app/model/ticket.dart';
import 'package:pretty_qr_code/pretty_qr_code.dart';
import 'package:stacked/stacked.dart';
import 'package:url_launcher/url_launcher_string.dart';

import 'bottom_sheet_ticket_model.dart';

class BottomSheetTicket extends StackedView<BottomSheetTicketModel> {
  final TicketModel ticket;
  const BottomSheetTicket({super.key, required this.ticket});

  @override
  Widget builder(BuildContext context, BottomSheetTicketModel viewModel, Widget? child) {
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
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20.0, vertical: 10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(height: 20),
                  Center(
                    child: SizedBox(
                      width: MediaQuery.of(context).size.width * 0.5,
                      height: MediaQuery.of(context).size.width * 0.5,
                      child: AspectRatio(
                        aspectRatio: 1,
                        child: PrettyQrView.data(
                          data: ticket.qr ?? "",
                          decoration: const PrettyQrDecoration(
                            quietZone: PrettyQrQuietZone.zero,
                          ),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  AutoSizeText(
                    ticket.name ?? "",
                    style: const TextStyle(
                      color: Colors.black,
                      fontWeight: FontWeight.bold,
                      fontSize: 20,
                    ),
                    textAlign: TextAlign.center,
                    minFontSize: 0,
                    maxLines: 1,
                  ),
                  AutoSizeText(
                    ticket.description ?? "",
                    style: const TextStyle(
                      color: Colors.black,
                      fontSize: 14,
                    ),
                    textAlign: TextAlign.center,
                    minFontSize: 0,
                    maxLines: 1,
                  ),
                  const SizedBox(height: 20),
                  if ((ticket.internalRefId ?? "").contains("event:"))
                    ElevatedButton(
                      onPressed: () {
                        viewModel.goToEvent(ticket.internalRefId ?? "");
                      },
                      child: Text("bottomsheet.ticket.go_to_event".tr()),
                    ),
                  const SizedBox(height: 10),
                  if (false)
                    ElevatedButton.icon(
                      onPressed: () {
                        viewModel.addTicketToWallet(ticket);
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.black,
                      ),
                      icon: const Icon(Icons.wallet_membership, color: Colors.white),
                      label: const Text("AGGIUNGI AL WALLET"),
                    ),
                  TextButton(
                      onPressed: () {
                        if (ticket.link == null || ticket.link!.isEmpty) return;
                        launchUrlString(ticket.link!);
                      },
                      child: Text("bottomsheet.ticket.view_online".tr())),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  BottomSheetTicketModel viewModelBuilder(BuildContext context) => BottomSheetTicketModel();
}
