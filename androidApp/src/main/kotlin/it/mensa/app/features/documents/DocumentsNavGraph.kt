package it.mensa.app.features.documents

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLEncoder

object DocumentsRoutes {
    const val LIST = "documents/list"
    const val DETAIL = "documents/detail/{docId}"
    const val PDF = "documents/pdf/{urlEncoded}"
    fun detail(docId: String) = "documents/detail/$docId"
    fun pdf(url: String) = "documents/pdf/${URLEncoder.encode(url, "UTF-8")}"
}

fun NavGraphBuilder.documentsNavGraph(navController: NavController) {
    composable(
        route = DocumentsRoutes.LIST,
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) {
        AreaDocumentsScreen(
            onNavigateToDetail = { docId -> navController.navigate(DocumentsRoutes.detail(docId)) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = DocumentsRoutes.DETAIL,
        arguments = listOf(navArgument("docId") { type = NavType.StringType }),
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) { backStackEntry ->
        val docId = backStackEntry.arguments?.getString("docId").orEmpty()
        DocumentDetailScreen(
            docId = docId,
            onBack = { navController.popBackStack() },
            onOpenPdf = { url -> navController.navigate(DocumentsRoutes.pdf(url)) },
        )
    }

    composable(
        route = DocumentsRoutes.PDF,
        arguments = listOf(navArgument("urlEncoded") { type = NavType.StringType }),
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) { backStackEntry ->
        val encodedUrl = backStackEntry.arguments?.getString("urlEncoded").orEmpty()
        PdfViewerScreen(
            encodedUrl = encodedUrl,
            onBack = { navController.popBackStack() },
        )
    }
}
