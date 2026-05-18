package it.mensa.app.features.quid

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * quidModule — Koin module for the QUID magazine feature.
 *
 * Registered alongside [it.mensa.app.di.appModule] in MensaApplication.
 */
val quidModule = module {

    // Issues list
    viewModel { QuidIssuesViewModel() }

    // Single issue (issueId, optional name)
    viewModel { (issueId: Long, issueName: String) ->
        QuidIssueViewModel(issueId, issueName)
    }

    // Single article
    viewModel { (articleId: Long) ->
        QuidArticleViewModel(articleId)
    }

    // PDF viewer — needs Context to resolve cacheDir
    viewModel { (pdfUrl: String) ->
        QuidPdfViewModel(pdfUrl, androidContext())
    }
}
