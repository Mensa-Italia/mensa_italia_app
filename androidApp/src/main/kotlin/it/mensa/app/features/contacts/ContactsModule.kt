package it.mensa.app.features.contacts

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * contactsModule — Koin bindings for Contacts addon feature.
 */
val contactsModule = module {
    viewModel { ContactsAddonViewModel() }
}
