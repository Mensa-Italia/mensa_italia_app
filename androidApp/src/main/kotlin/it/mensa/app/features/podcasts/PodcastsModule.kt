package it.mensa.app.features.podcasts

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * podcastsModule — Koin bindings for Podcasts feature.
 */
val podcastsModule = module {
    viewModel { PodcastsListViewModel() }
    viewModel { (podcastId: String) -> PodcastEpisodesViewModel(podcastId) }
}
