import 'package:mensa_italia_app/model/ticket.dart';
import 'package:mensa_italia_app/services/tickets_see%20.dart';
import 'package:mensa_italia_app/ui/common/master_model.dart';
import 'package:mensa_italia_app/ui/widgets/common/bottom_sheet_ticket/bottom_sheet_ticket.dart';
import 'package:stacked/stacked.dart';

class TicketsViewModel extends MasterModel {
  @override
  String componentName = "views.tickets.title";
  List<TicketModel> get tickets => TicketSSE().tickets;

  TicketsViewModel() {
    TicketSSE().addListener(rebuildUi);
  }

  @override
  void dispose() {
    TicketSSE().removeListener(rebuildUi);
    super.dispose();
  }

  Function() tapOnTicket(int index) {
    final ticket = tickets[index];
    return () {
      showBeautifulBottomSheet(
        child: BottomSheetTicket(
          ticket: ticket,
          underPageTitle: componentName,
        ),
      );
    };
  }
}
