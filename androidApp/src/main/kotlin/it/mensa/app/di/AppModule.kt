package it.mensa.app.di

import it.mensa.app.features.auth.LoginViewModel
import it.mensa.app.features.card.CardViewModel
import it.mensa.app.features.discover.DiscoverViewModel
import it.mensa.app.features.profile.ProfileViewModel
import it.mensa.app.features.profile.sub.CalendarLinkerViewModel
import it.mensa.app.features.profile.sub.DevicesViewModel
import it.mensa.app.features.profile.sub.LanguagePickerViewModel
import it.mensa.app.features.profile.sub.MakeDonationViewModel
import it.mensa.app.features.profile.sub.OrgChartViewModel
import it.mensa.app.features.profile.sub.PaymentMethodPickerViewModel
import it.mensa.app.features.profile.sub.PaymentMethodsViewModel
import it.mensa.app.features.profile.sub.RenewMembershipViewModel
import it.mensa.app.features.search.SearchViewModel
import it.mensa.app.features.today.TodayViewModel
import it.mensa.app.ui.root.RootViewModel
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.calendar.CalendarHelper
import it.mensa.app.services.location.LocationProvider
import it.mensa.app.services.push.PushTokenStore
import it.mensa.app.services.stripe.StripeService
import it.mensa.app.services.wallet.WalletService
import it.mensa.app.support.LocaleManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * AppModule — Koin module for Android-specific services.
 *
 * All shared-module bindings (repositories, auth, i18n) are registered
 * by [it.mensa.shared.MensaSdk.initKoin] and must NOT be duplicated here.
 *
 * This module registers only the Android-specific service layer:
 * - Location (FusedLocation)
 * - Push token persistence (DataStore)
 * - Audio player controller (Media3)
 * - Calendar helper (CalendarContract)
 * - Stripe payment service
 * - Google Wallet service
 * - Locale manager (DataStore)
 */
val appModule = module {

    // Location
    single { LocationProvider(androidContext()) }

    // Push token store (DataStore-backed)
    single { PushTokenStore(androidContext()) }

    // Audio player controller (singleton — owns MediaController connection)
    single { AudioPlayerController(androidContext()) }

    // Calendar helper
    single { CalendarHelper(androidContext()) }

    // Stripe service — pulls publishable key + intent client secrets from the
    // shared PaymentMethodsRepository registered by MensaSdk.
    single { StripeService(androidContext(), get()) }

    // Google Wallet service
    single { WalletService(androidContext()) }

    // Locale manager (DataStore-backed)
    single { LocaleManager(androidContext()) }

    // ViewModels
    viewModel { RootViewModel() }
    viewModel { LoginViewModel() }
    viewModel { CardViewModel() }
    viewModel { DiscoverViewModel() }
    viewModel { TodayViewModel(get()) }
    viewModel { SearchViewModel(androidContext()) }

    // Profile ViewModels
    viewModel { ProfileViewModel(get()) }
    viewModel { LanguagePickerViewModel(get()) }
    viewModel { PaymentMethodsViewModel() }
    viewModel { PaymentMethodPickerViewModel() }
    viewModel { RenewMembershipViewModel() }
    viewModel { MakeDonationViewModel() }
    viewModel { CalendarLinkerViewModel() }
    viewModel { DevicesViewModel() }
    viewModel { OrgChartViewModel() }
}
