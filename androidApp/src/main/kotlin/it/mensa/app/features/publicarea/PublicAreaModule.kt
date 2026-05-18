package it.mensa.app.features.publicarea

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val publicAreaModule = module {
    viewModel { PublicLocalOfficesListViewModel() }
    viewModel { (officeId: String) -> PublicLocalOfficeDetailViewModel(officeId) }
}
