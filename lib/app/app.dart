import 'package:mensa_italia_app/ui/bottom_sheets/notice/notice_sheet.dart';
import 'package:mensa_italia_app/ui/dialogs/info_alert/info_alert_dialog.dart';
import 'package:mensa_italia_app/ui/views/login/login_view.dart';
import 'package:mensa_italia_app/ui/views/startup/startup_view.dart';
import 'package:stacked/stacked_annotations.dart';
import 'package:stacked_services/stacked_services.dart';
import 'package:mensa_italia_app/ui/views/home/home_view.dart';
import 'package:mensa_italia_app/ui/views/external_addon_webview/external_addon_webview_view.dart';
import 'package:mensa_italia_app/ui/views/addon_contacts/addon_contacts_view.dart';
import 'package:mensa_italia_app/ui/views/renew_membership/renew_membership_view.dart';
import 'package:mensa_italia_app/ui/views/generic_webview/generic_webview_view.dart';
import 'package:mensa_italia_app/ui/views/addon_test_assistant/addon_test_assistant_view.dart';
import 'package:mensa_italia_app/ui/views/addon_area_documents/addon_area_documents_view.dart';
import 'package:mensa_italia_app/ui/views/events_map/events_map_view.dart';
import 'package:mensa_italia_app/ui/views/add_event/add_event_view.dart';
import 'package:mensa_italia_app/ui/views/map_picker/map_picker_view.dart';
import 'package:mensa_italia_app/ui/views/document_viewer/document_viewer_view.dart';
// @stacked-import

@StackedApp(
  routes: [
    MaterialRoute(page: LoginView),
    MaterialRoute(page: StartupView),
    MaterialRoute(page: HomeView),
    MaterialRoute(page: ExternalAddonWebviewView),
    MaterialRoute(page: AddonContactsView),
    MaterialRoute(page: RenewMembershipView),
    MaterialRoute(page: GenericWebviewView),
    MaterialRoute(page: AddonTestAssistantView),
    MaterialRoute(page: AddonAreaDocumentsView),
    MaterialRoute(page: EventsMapView),
    MaterialRoute(page: AddEventView),
    MaterialRoute(page: MapPickerView),
    MaterialRoute(page: DocumentViewerView),
// @stacked-route
  ],
  dependencies: [
    LazySingleton(classType: BottomSheetService),
    LazySingleton(classType: DialogService),
    LazySingleton(classType: NavigationService),
    // @stacked-service
  ],
  bottomsheets: [
    StackedBottomsheet(classType: NoticeSheet),
    // @stacked-bottom-sheet
  ],
  dialogs: [
    StackedDialog(classType: InfoAlertDialog),
// @stacked-dialog
  ],
)
class App {}
