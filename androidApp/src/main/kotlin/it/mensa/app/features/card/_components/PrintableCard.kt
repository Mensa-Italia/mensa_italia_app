package it.mensa.app.features.card._components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.ui.theme.MensaBlue
import it.mensa.app.ui.theme.MensaCyan

/**
 * Printable / shareable static render of the membership card. Mirrors iOS
 * `PrintableCardView`: fixed 540×340 layout, no animations, captured by
 * [shareCardImage] into a PNG bitmap.
 */
@Composable
fun PrintableCard(
    fullName: String,
    memberId: String,
    expiry: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(20.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(MensaBlue, MensaCyan))),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = "MENSA ITALIA",
                            color = Color.White.copy(alpha = 0.85f),
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Tessera socio",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "M",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                Column {
                    Text(
                        text = "TITOLARE",
                        color = Color.White.copy(alpha = 0.7f),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = fullName.ifBlank { "Socio Mensa" },
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = "ID SOCIO",
                            color = Color.White.copy(alpha = 0.7f),
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = memberId.ifBlank { "—" },
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                            ),
                        )
                    }
                    if (expiry.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SCADENZA",
                                color = Color.White.copy(alpha = 0.7f),
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.5.sp,
                                ),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = expiry,
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
