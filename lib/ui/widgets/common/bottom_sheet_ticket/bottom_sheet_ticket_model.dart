import 'package:flutter_wallet_card/flutter_wallet_card.dart';
import 'package:flutter_wallet_card/models/wallet_card.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/app/app.router.dart';
import 'package:mensa_italia_app/model/ticket.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:stacked/stacked.dart';

class BottomSheetTicketModel extends MasterModel {
  final String underPageTitle;

  BottomSheetTicketModel({required this.underPageTitle});

  void goToEvent(String s) {
    Api().getEvent(s.replaceFirst("event:", "")).then((event) {
      navigationService.navigateToEventShowcaseView(event: event, previousPageTitle: underPageTitle,);
    });
  }

  void addTicketToWallet(TicketModel ticket) async {
    bool isAvailable = await FlutterWalletCard.isWalletAvailable;

    if (isAvailable) {
      // Create a wallet card
      final card = WalletCard(
        id: 'ticket_${ticket.id}',
        type: WalletCardType.eventTicket,
        platformData: {
          // iOS specific data
          'passTypeIdentifier': 'pass.app.mensa.it',
          'teamIdentifier': '6WA5D3RJBU',
          // Android specific data
          'issuerId': 'your-issuer-id',
          'classId': 'your-class-id',
        },
        metadata: WalletCardMetadata(
          title: ticket.name ?? "Biglietto",
          description: ticket.description ?? "",
          organizationName: "Mensa Italia",
          serialNumber: ticket.qr ?? "000000",
        ),
      );

      // Add card to wallet
      bool success = await FlutterWalletCard.addToWallet(card);

      if (success) {
        print('Card added successfully!');
      }
    }
  }
}
