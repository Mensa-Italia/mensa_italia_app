package it.mensa.app.features.testassistant

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * testAssistantModule — Koin bindings for TestAssistant feature.
 *
 * Register alongside appModule in [it.mensa.app.MensaApplication.onCreate].
 */
val testAssistantModule = module {
    viewModel { TestAssistantViewModel() }
}
