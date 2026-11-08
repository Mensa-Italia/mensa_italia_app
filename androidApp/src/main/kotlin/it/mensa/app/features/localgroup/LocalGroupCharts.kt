package it.mensa.app.features.localgroup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// I grafici delle statistiche della sede. Nel progetto non c'e' una libreria
// di grafici: barre e linee sono disegnate su un Canvas, come la curva della
// classifica. Sul Canvas niente testo: valori ed etichette dei mesi sono Text
// in righe a pesi uguali, una colonna per mese, allineate alle barre.

/** One bar per month, [values] in [color]; the values over the chart. */
@Composable
internal fun MonthlyBars(values: List<Int>, labels: List<String>, color: Color) {
    ChartFrame(values = values, labels = labels) {
        Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) { drawBars(listOf(values), listOf(color)) }
    }
}

/** A line through the months' [values], with a point on each. */
@Composable
internal fun MonthlyLine(values: List<Int>, labels: List<String>) {
    val color = MaterialTheme.colorScheme.primary
    val baseline = MaterialTheme.colorScheme.outlineVariant
    ChartFrame(values = values, labels = labels) {
        Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) { drawLine(values, color, baseline) }
    }
}

/** Two bars per month side by side, [a] in primary and [b] in tertiary, with a legend. */
@Composable
internal fun MonthlyPairedBars(a: List<Int>, b: List<Int>, labels: List<String>, labelA: String, labelB: String) {
    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.tertiary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendEntry(colorA, labelA)
            LegendEntry(colorB, labelB)
        }
        ChartFrame(values = null, labels = labels) {
            Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) { drawBars(listOf(a, b), listOf(colorA, colorB)) }
        }
    }
}

/** The values row (when few enough to read), the chart, the months row. */
@Composable
private fun ChartFrame(values: List<Int>?, labels: List<String>, chart: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (values != null && values.size <= MAX_READABLE_VALUES) {
            ColumnsRow(values.map(Int::toString), MaterialTheme.colorScheme.onSurface)
        }
        chart()
        ColumnsRow(labels, MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ColumnsRow(texts: List<String>, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        texts.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LegendEntry(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Bars of every series per month, grouped in the middle of the month's column. */
private fun DrawScope.drawBars(series: List<List<Int>>, colors: List<Color>) {
    val months = series.maxOf { it.size }
    if (months == 0) return
    val max = series.flatten().maxOrNull()?.coerceAtLeast(1) ?: 1
    val slot = size.width / months
    // Con pochi mesi le barre resterebbero larghissime: si fermano a BAR_MAX_WIDTH.
    val group = minOf(slot * GROUP_FILL, BAR_MAX_WIDTH.toPx() * series.size)
    val bar = group / series.size
    val radius = CornerRadius(BAR_RADIUS.toPx())
    for (month in 0 until months) {
        val start = slot * month + (slot - group) / 2
        series.forEachIndexed { index, values ->
            val value = values.getOrElse(month) { 0 }
            val height = size.height * value / max
            if (height > 0f) {
                drawRoundRect(
                    color = colors[index],
                    topLeft = Offset(start + bar * index + BAR_GAP.toPx() / 2, size.height - height),
                    size = Size(bar - BAR_GAP.toPx(), height),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/** The line through [values] (scaled between their minimum and maximum) and its points. */
private fun DrawScope.drawLine(values: List<Int>, color: Color, baseline: Color) {
    if (values.isEmpty()) return
    val min = values.min()
    val span = (values.max() - min).takeIf { it > 0 }
    val inset = POINT_RADIUS.toPx() * 2
    val slot = size.width / values.size
    val points = values.mapIndexed { index, value ->
        val share = span?.let { (value - min).toFloat() / it } ?: HALF
        Offset(slot * index + slot / 2, size.height - inset - (size.height - inset * 2) * share)
    }
    drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
    val path = Path().apply {
        points.forEachIndexed { index, point -> if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y) }
    }
    drawPath(path, color, style = Stroke(width = LINE_WIDTH.toPx()))
    points.forEach { drawCircle(color, POINT_RADIUS.toPx(), it) }
}

private val CHART_HEIGHT = 160.dp
private val BAR_RADIUS = 4.dp
private val BAR_MAX_WIDTH = 28.dp
private val BAR_GAP = 2.dp
private val LINE_WIDTH = 2.5.dp
private val POINT_RADIUS = 4.dp
private const val GROUP_FILL = 0.6f
private const val HALF = 0.5f
private const val MAX_READABLE_VALUES = 12
