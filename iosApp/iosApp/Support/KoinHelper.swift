import Foundation
import Shared

/// Swift-side convenience facade over the Kotlin `KoinAccess` instance.
/// All repositories registered in the shared Koin graph are exposed here as
/// computed properties so SwiftUI views can write e.g. `koin.tickets` and get
/// a strongly-typed Kotlin repository without going through `KoinPlatform`.
///
/// The global `koin` singleton (defined in `KoinAccess+Singleton.swift`) already
/// wraps `KoinHelperKt.koinAccess()`; this file simply documents and verifies
/// that every new repository is reachable.
enum KoinHelper {
    /// Returns the singleton `KoinAccess`. Identical to the global `koin`.
    static var access: KoinAccess { koin }

    // MARK: - Convenience accessors mirroring the Kotlin contract.
    static var auth: AuthRepository { koin.auth }
    static var events: EventsRepository { koin.events }
    static var eventSchedules: EventSchedulesRepository { koin.eventSchedules }
    static var deals: DealsRepository { koin.deals }
    static var sigs: SigsRepository { koin.sigs }
    static var stamps: StampsRepository { koin.stamps }
    static var notifications: NotificationsRepository { koin.notifications }
    static var locations: LocationsRepository { koin.locations }
    static var addons: AddonsRepository { koin.addons }
    static var search: SearchRepository { koin.search }
    static var regSoci: RegSociRepository { koin.regSoci }
    static var boutique: BoutiqueRepository { koin.boutique }
    static var quid: QuidRepository { koin.quid }
    static var mensaTest: MensaTestClient { koin.mensaTest }
    static var podcasts: PodcastsRepository { koin.podcasts }
    static var documents: DocumentsRepository { koin.documents }
    static var tickets: TicketsRepository { koin.tickets }
    static var receipts: ReceiptsRepository { koin.receipts }
    static var devices: DevicesRepository { koin.devices }
    static var calendarLinks: CalendarLinksRepository { koin.calendarLinks }
    static var paymentMethods: PaymentMethodsRepository { koin.paymentMethods }
    static var metadata: MetadataRepository { koin.metadata }
    static var i18n: I18n { koin.i18n }
    static var onboarding: OnboardingState { koin.onboarding }
}
