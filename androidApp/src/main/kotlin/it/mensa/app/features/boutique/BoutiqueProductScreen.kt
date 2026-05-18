package it.mensa.app.features.boutique

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.BoutiqueModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BoutiqueProductScreen(
    productId: String,
    onBack: () -> Unit,
    vm: BoutiqueProductViewModel = koinViewModel(parameters = { parametersOf(productId) }),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showContactAlert by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.product?.name
                            ?: tr("addons.boutique.product", fallback = "Prodotto"),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.back", fallback = "Indietro"),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val product = uiState.product
        if (product == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            ProductContent(
                product = product,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onOrder = {
                    val url = vm.extractOrderUrl(product)
                    if (url != null) {
                        runCatching {
                            CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                        }.onFailure {
                            // Fallback to plain Intent
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    } else {
                        showContactAlert = true
                    }
                },
            )
        }
    }

    if (showContactAlert) {
        AlertDialog(
            onDismissRequest = { showContactAlert = false },
            title = {
                Text(tr("addons.boutique.contact.title", fallback = "Contatta Mensa per ordinare"))
            },
            text = {
                Text(
                    tr(
                        "addons.boutique.contact.message",
                        fallback = "Per acquistare questo prodotto contatta la segreteria nazionale.",
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { showContactAlert = false }) { Text("OK") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductContent(
    product: BoutiqueModel,
    onOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val images = product.image.filter { it.isNotEmpty() }
    val pagerState = rememberPagerState(pageCount = { maxOf(images.size, 1) })

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Hero gallery (HorizontalPager)
        if (images.isNotEmpty()) {
            Column {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(22.dp)),
                ) { page ->
                    val url = FilesUrl.build(
                        collection = "boutique",
                        recordId = product.id,
                        filename = images[page],
                        thumb = "1200x900",
                    )
                    CachedAsyncImage(
                        model = url,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Page indicator
                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        repeat(images.size) { idx ->
                            val isCurrent = pagerState.currentPage == idx
                            Surface(
                                shape = CircleShape,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isCurrent) 7.dp else 5.dp),
                            ) {}
                        }
                    }
                }
            }
        }

        // Name + price
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = formatPrice(product.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // alternativeOf badge
        if (product.alternativeOf.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(
                    width = 0.dp,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Text(
                    text = product.alternativeOf,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        // Description
        if (product.description.isNotEmpty()) {
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Order CTA
        Button(
            onClick = onOrder,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = tr("addons.boutique.order_now", fallback = "Ordina ora"),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatPrice(amount: Int): String {
    return try {
        val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
        fmt.maximumFractionDigits = 2
        fmt.minimumFractionDigits = 0
        fmt.format(amount)
    } catch (_: Exception) {
        "€ $amount"
    }
}
