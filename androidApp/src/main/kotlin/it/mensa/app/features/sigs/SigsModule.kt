package it.mensa.app.features.sigs

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val sigsModule = module {
    viewModel { SigListViewModel() }
    viewModel { (id: String) -> SigDetailViewModel(id) }
    viewModel { (id: String?) -> AddSigViewModel(id) }
}
