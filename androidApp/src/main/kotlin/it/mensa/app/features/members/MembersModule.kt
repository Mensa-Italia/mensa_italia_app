package it.mensa.app.features.members

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val membersModule = module {
    viewModel { MembersDirectoryViewModel() }
    viewModel { (id: String) -> MemberDetailViewModel(id) }
}
