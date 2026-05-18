package it.mensa.app.features.documents

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * documentsModule — Koin bindings for Documents feature.
 */
val documentsModule = module {
    viewModel { AreaDocumentsViewModel() }
    viewModel { (docId: String) -> DocumentDetailViewModel(docId) }
    viewModel { (url: String) -> PdfViewerViewModel(url) }
}
