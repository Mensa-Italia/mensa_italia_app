package it.mensa.shared

import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.i18n.I18n
import it.mensa.shared.onboarding.OnboardingState
import it.mensa.shared.repository.AddonsRepository
import it.mensa.shared.repository.BoutiqueRepository
import it.mensa.shared.repository.CalendarLinksRepository
import it.mensa.shared.repository.DealsRepository
import it.mensa.shared.repository.DevicesRepository
import it.mensa.shared.repository.DocumentsRepository
import it.mensa.shared.repository.ExAppsRepository
import it.mensa.shared.repository.EventSchedulesRepository
import it.mensa.shared.repository.EventsRepository
import it.mensa.shared.repository.LocationsRepository
import it.mensa.shared.repository.MetadataRepository
import it.mensa.shared.repository.OrgChartRepository
import it.mensa.shared.repository.NotificationsRepository
import it.mensa.shared.repository.PaymentMethodsRepository
import it.mensa.shared.repository.ReceiptsRepository
import it.mensa.shared.repository.RegSociRepository
import it.mensa.shared.repository.SearchRepository
import it.mensa.shared.repository.SigsRepository
import it.mensa.shared.repository.StampsRepository
import it.mensa.shared.repository.LocalOfficesRepository
import it.mensa.shared.repository.PodcastsRepository
import it.mensa.shared.iqtest.MensaTestClient
import it.mensa.shared.repository.QuidRepository
import it.mensa.shared.repository.TicketsRepository
import it.mensa.shared.spotlight.SpotlightSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.mp.KoinPlatform

class KoinAccess(
    val auth: AuthRepository,
    val events: EventsRepository,
    val eventSchedules: EventSchedulesRepository,
    val deals: DealsRepository,
    val sigs: SigsRepository,
    val stamps: StampsRepository,
    val notifications: NotificationsRepository,
    val locations: LocationsRepository,
    val addons: AddonsRepository,
    val search: SearchRepository,
    val regSoci: RegSociRepository,
    val boutique: BoutiqueRepository,
    val documents: DocumentsRepository,
    val tickets: TicketsRepository,
    val receipts: ReceiptsRepository,
    val devices: DevicesRepository,
    val calendarLinks: CalendarLinksRepository,
    val paymentMethods: PaymentMethodsRepository,
    val metadata: MetadataRepository,
    val orgChart: OrgChartRepository,
    val quid: QuidRepository,
    val podcasts: PodcastsRepository,
    val localOffices: LocalOfficesRepository,
    val exApps: ExAppsRepository,
    val i18n: I18n,
    val onboarding: OnboardingState,
    val mensaTest: MensaTestClient,
    val spotlightSync: SpotlightSyncEngine,
)

fun koinAccess(): KoinAccess = KoinAccess(
    auth = KoinPlatform.getKoin().get(),
    events = KoinPlatform.getKoin().get(),
    eventSchedules = KoinPlatform.getKoin().get(),
    deals = KoinPlatform.getKoin().get(),
    sigs = KoinPlatform.getKoin().get(),
    stamps = KoinPlatform.getKoin().get(),
    notifications = KoinPlatform.getKoin().get(),
    locations = KoinPlatform.getKoin().get(),
    addons = KoinPlatform.getKoin().get(),
    search = KoinPlatform.getKoin().get(),
    regSoci = KoinPlatform.getKoin().get(),
    boutique = KoinPlatform.getKoin().get(),
    documents = KoinPlatform.getKoin().get(),
    tickets = KoinPlatform.getKoin().get(),
    receipts = KoinPlatform.getKoin().get(),
    devices = KoinPlatform.getKoin().get(),
    calendarLinks = KoinPlatform.getKoin().get(),
    paymentMethods = KoinPlatform.getKoin().get(),
    metadata = KoinPlatform.getKoin().get(),
    orgChart = KoinPlatform.getKoin().get(),
    quid = KoinPlatform.getKoin().get(),
    podcasts = KoinPlatform.getKoin().get(),
    localOffices = KoinPlatform.getKoin().get(),
    exApps = KoinPlatform.getKoin().get(),
    i18n = KoinPlatform.getKoin().get(),
    onboarding = KoinPlatform.getKoin().get(),
    mensaTest = KoinPlatform.getKoin().get(),
    spotlightSync = KoinPlatform.getKoin().get(),
)

fun appScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default)
