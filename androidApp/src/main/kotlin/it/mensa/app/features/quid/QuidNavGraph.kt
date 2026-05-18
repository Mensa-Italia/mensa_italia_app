package it.mensa.app.features.quid

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder

// ─── Route constants ─────────────────────────────────────────────────────────

object QuidRoute {
    const val ISSUES = "quid/list"
    const val ISSUE = "quid/issue/{issueId}/{issueName}"
    const val ARTICLE = "quid/article/{articleId}"
    const val PDF = "quid/pdf/{pdfUrl}/{pdfTitle}"

    const val ARG_ISSUE_ID = "issueId"
    const val ARG_ISSUE_NAME = "issueName"
    const val ARG_ARTICLE_ID = "articleId"
    const val ARG_PDF_URL = "pdfUrl"
    const val ARG_PDF_TITLE = "pdfTitle"

    fun issue(issueId: Long, issueName: String) =
        "quid/issue/$issueId/${URLEncoder.encode(issueName, "UTF-8")}"

    fun article(articleId: Long) = "quid/article/$articleId"

    fun pdf(pdfUrl: String, title: String) =
        "quid/pdf/${URLEncoder.encode(pdfUrl, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
}

// ─── Nav graph builder ────────────────────────────────────────────────────────

/**
 * quidNavGraph — registers all QUID routes into the given [NavGraphBuilder].
 *
 * Routes:
 * - [QuidRoute.ISSUES]   → QuidIssuesScreen (list of issues)
 * - [QuidRoute.ISSUE]    → QuidIssueScreen (articles in issue)
 * - [QuidRoute.ARTICLE]  → QuidArticleScreen (article reader)
 * - [QuidRoute.PDF]      → QuidPdfScreen (PDF viewer)
 *
 * Usage:
 * ```
 * NavHost(...) {
 *     quidNavGraph(navController)
 * }
 * ```
 */
fun NavGraphBuilder.quidNavGraph(navController: NavController) {

    // Issues list
    composable(QuidRoute.ISSUES) {
        QuidIssuesScreen(
            onNavigateToIssue = { issueId, issueName ->
                navController.navigate(QuidRoute.issue(issueId, issueName))
            },
            onNavigateToPdf = { pdfUrl, title ->
                navController.navigate(QuidRoute.pdf(pdfUrl, title))
            },
            onBack = { navController.popBackStack() },
        )
    }

    // Single issue articles
    composable(
        route = QuidRoute.ISSUE,
        arguments = listOf(
            navArgument(QuidRoute.ARG_ISSUE_ID) { type = NavType.LongType },
            navArgument(QuidRoute.ARG_ISSUE_NAME) { type = NavType.StringType },
        ),
    ) { backStack ->
        val issueId = backStack.arguments?.getLong(QuidRoute.ARG_ISSUE_ID) ?: return@composable
        val issueName = backStack.arguments?.getString(QuidRoute.ARG_ISSUE_NAME)
            ?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
        QuidIssueScreen(
            issueId = issueId,
            issueName = issueName,
            onBack = { navController.popBackStack() },
            onNavigateToArticle = { articleId ->
                navController.navigate(QuidRoute.article(articleId))
            },
        )
    }

    // Article reader
    composable(
        route = QuidRoute.ARTICLE,
        arguments = listOf(
            navArgument(QuidRoute.ARG_ARTICLE_ID) { type = NavType.LongType },
        ),
    ) { backStack ->
        val articleId = backStack.arguments?.getLong(QuidRoute.ARG_ARTICLE_ID) ?: return@composable
        QuidArticleScreen(
            articleId = articleId,
            onBack = { navController.popBackStack() },
        )
    }

    // PDF viewer
    composable(
        route = QuidRoute.PDF,
        arguments = listOf(
            navArgument(QuidRoute.ARG_PDF_URL) { type = NavType.StringType },
            navArgument(QuidRoute.ARG_PDF_TITLE) { type = NavType.StringType },
        ),
    ) { backStack ->
        val pdfUrl = backStack.arguments?.getString(QuidRoute.ARG_PDF_URL)
            ?.let { URLDecoder.decode(it, "UTF-8") } ?: return@composable
        val title = backStack.arguments?.getString(QuidRoute.ARG_PDF_TITLE)
            ?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
        QuidPdfScreen(
            pdfUrl = pdfUrl,
            title = title,
            onBack = { navController.popBackStack() },
        )
    }
}
