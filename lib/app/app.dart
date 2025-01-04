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
import 'package:mensa_italia_app/ui/views/event_calendar/event_calendar_view.dart';
import 'package:mensa_italia_app/ui/views/calendar_linker/calendar_linker_view.dart';
import 'package:mensa_italia_app/ui/views/event_showcase/event_showcase_view.dart';
import 'package:mensa_italia_app/ui/views/add_event_schedule_list/add_event_schedule_list_view.dart';
import 'package:mensa_italia_app/ui/views/add_schedule/add_schedule_view.dart';
import 'package:mensa_italia_app/ui/views/addon_deals/addon_deals_view.dart';
import 'package:mensa_italia_app/ui/views/addon_deals_details/addon_deals_details_view.dart';
import 'package:mensa_italia_app/ui/views/addon_deals_add/addon_deals_add_view.dart';
import 'package:mensa_italia_app/ui/dialogs/input_text/input_text_dialog.dart';
import 'package:mensa_italia_app/ui/views/addon_stamp/addon_stamp_view.dart';
import 'package:mensa_italia_app/ui/views/notification_manager/notification_manager_view.dart';
import 'package:mensa_italia_app/ui/views/payment_method_manager/payment_method_manager_view.dart';
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
    MaterialRoute(page: EventCalendarView),
    MaterialRoute(page: CalendarLinkerView),
    MaterialRoute(page: EventShowcaseView),
    MaterialRoute(page: AddEventScheduleListView),
    MaterialRoute(page: AddScheduleView),
    MaterialRoute(page: AddonDealsView),
    MaterialRoute(page: AddonDealsDetailsView),
    MaterialRoute(page: AddonDealsAddView),
    MaterialRoute(page: AddonStampView),
    MaterialRoute(page: NotificationManagerView),
    MaterialRoute(page: PaymentMethodManagerView),
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
    StackedDialog(classType: InputTextDialog),
// @stacked-dialog
  ],
)
class App {}
