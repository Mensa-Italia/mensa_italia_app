package it.mensa.app.features.localoffices

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val localOfficesModule = module {
    viewModel { LocalOfficesListViewModel() }
    viewModel { (id: String) -> LocalOfficeViewModel(id) }
    viewModel { (id: String) -> LocalOfficeLinktreeViewModel(id) }
}
