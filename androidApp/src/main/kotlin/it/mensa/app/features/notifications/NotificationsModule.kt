package it.mensa.app.features.notifications

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * notificationsModule — Koin bindings for the Notifications feature.
 *
 * Registered alongside [it.mensa.app.di.appModule] in MensaApplication.
 * NotificationDetailViewModel uses parametrized injection for notification ID.
 */
val notificationsModule = module {
    viewModel { NotificationsListViewModel() }
    viewModel { (id: String) -> NotificationDetailViewModel(id) }
    viewModel { NotificationManagerViewModel() }
    single { AccountConfirmationController() }
}
