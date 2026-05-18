package it.mensa.app.features.discover._components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.features.discover.DiscoverCategory
import it.mensa.app.support.tr

/**
 * DiscoverBrokenGrid — broken-grid layout for one Discover section.
 *
 * NOT a uniform grid. The layout is **driven by the count of categories** and
 * selects from a small set of hand-arranged patterns, each mixing one Hero tile
 * with smaller stacked tiles. Tonal variants cycle so no two adjacent tiles
 * share a color.
 *
 * Patterns by count:
 *   - 2  →  side-by-side medium pair (55/45)
 *   - 3  →  hero (60%) + 2 stacked smalls (40%)
 *   - 4  →  hero (60%) + 2 stacked smalls (40%) on row 1, full-width medium on row 2
 *   - 5  →  hero (60%) + 2 stacked smalls (40%) on row 1, two mediums (55/45) on row 2
 *   - 6  →  hero (60%) + 2 stacked smalls (40%) on row 1, two stacked smalls (40%) + hero (60%) on row 2
 *
 * When [mirror] is true the hero swaps to the trailing edge — useful for
 * alternating the focal side between sections so the screen feels rhythmic.
 *
 * @param categories ordered list of categories for this section
 * @param sectionKicker optional kicker label shown only on the hero tile (e.g. "COMUNITÀ")
 * @param tonalPalette per-section tonal cycle. Length must be ≥ categories.size.
 * @param onCategoryClick click handler — receives the [DiscoverCategory]
 * @param globalOffset cross-section stagger offset for entrance animations
 * @param mirror flip the layout so the hero sits on the right
 */
@Composable
fun DiscoverBrokenGrid(
    categories: List<DiscoverCategory>,
    sectionKicker: String,
    tonalPalette: List<DiscoverTonal>,
    onCategoryClick: (DiscoverCategory) -> Unit,
    modifier: Modifier = Modifier,
    globalOffset: Int = 0,
    mirror: Boolean = false,
) {
    if (categories.isEmpty()) return

    val spec = remember(categories.size, mirror) {
        val base = layoutSpecFor(categories.size)
        if (mirror) base.mirrored() else base
    }
    val palette = ensureNoAdjacentRepeats(tonalPalette, categories.size)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        spec.rows.forEach { rowSpec ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowSpec.height),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowSpec.columns.forEach { colSpec ->
                    when (colSpec) {
                        is ColumnSpec.Single -> {
                            val tileIndex = colSpec.indexOffset
                            if (tileIndex >= categories.size) return@forEach
                            val category = categories[tileIndex]
                            val tonal = palette[tileIndex]
                            val showKicker = tileIndex == 0
                            DiscoverExpressiveTile(
                                kicker = if (showKicker) sectionKicker else null,
                                label = tr(category.labelKey, fallback = category.labelFallback),
                                icon = category.icon,
                                tonal = tonal,
                                corners = colSpec.corners,
                                size = colSpec.size,
                                onClick = { onCategoryClick(category) },
                                entranceIndex = globalOffset + tileIndex,
                                modifier = Modifier
                                    .weight(colSpec.weight)
                                    .fillMaxHeight(),
                            )
                        }
                        is ColumnSpec.Stack -> {
                            Column(
                                modifier = Modifier
                                    .weight(colSpec.weight)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                colSpec.tiles.forEach { stackTile ->
                                    val stackTileIndex = stackTile.indexOffset
                                    if (stackTileIndex >= categories.size) return@forEach
                                    val sCategory = categories[stackTileIndex]
                                    val sTonal = palette[stackTileIndex]
                                    DiscoverExpressiveTile(
                                        kicker = null,
                                        label = tr(sCategory.labelKey, fallback = sCategory.labelFallback),
                                        icon = sCategory.icon,
                                        tonal = sTonal,
                                        corners = stackTile.corners,
                                        size = stackTile.size,
                                        onClick = { onCategoryClick(sCategory) },
                                        entranceIndex = globalOffset + stackTileIndex,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Layout spec ─────────────────────────────────────────────────────────────

private data class LayoutSpec(
    val rows: List<RowSpec>,
)

private data class RowSpec(
    val height: Dp,
    val columns: List<ColumnSpec>,
)

private sealed class ColumnSpec {
    abstract val weight: Float

    data class Single(
        override val weight: Float,
        val size: DiscoverTileSize,
        val corners: TileCorners,
        /** Flat index of this tile within the section's categories list. */
        val indexOffset: Int,
    ) : ColumnSpec()

    data class Stack(
        override val weight: Float,
        val tiles: List<StackTile>,
    ) : ColumnSpec()
}

private data class StackTile(
    val size: DiscoverTileSize,
    val corners: TileCorners,
    /** Flat index of this tile within the section's categories list. */
    val indexOffset: Int,
)

// ─── Pattern presets ─────────────────────────────────────────────────────────

private fun layoutSpecFor(count: Int): LayoutSpec = when (count) {
    1 -> oneTile()
    2 -> twoTiles()
    3 -> threeTiles()
    4 -> fourTiles()
    5 -> fiveTiles()
    6 -> sixTiles()
    else -> sixTiles().extendedWith(count - 6)
}

/** Single full-width medium. */
private fun oneTile() = LayoutSpec(
    rows = listOf(
        RowSpec(
            height = 140.dp,
            columns = listOf(
                ColumnSpec.Single(
                    weight = 1f,
                    size = DiscoverTileSize.Medium,
                    corners = TileCorners(28.dp, 12.dp, 12.dp, 28.dp),
                    indexOffset = 0,
                ),
            ),
        ),
    ),
)

/** Side-by-side medium pair, 55/45 weight, mirrored corners. */
private fun twoTiles() = LayoutSpec(
    rows = listOf(
        RowSpec(
            height = 140.dp,
            columns = listOf(
                ColumnSpec.Single(
                    weight = 1.15f,
                    size = DiscoverTileSize.Medium,
                    corners = TileCorners(28.dp, 12.dp, 12.dp, 12.dp),
                    indexOffset = 0,
                ),
                ColumnSpec.Single(
                    weight = 0.95f,
                    size = DiscoverTileSize.Medium,
                    corners = TileCorners(12.dp, 12.dp, 28.dp, 12.dp),
                    indexOffset = 1,
                ),
            ),
        ),
    ),
)

/** Hero (60%) + 2 stacked smalls (40%). */
private fun threeTiles() = LayoutSpec(
    rows = listOf(
        RowSpec(
            height = 224.dp,
            columns = listOf(
                ColumnSpec.Single(
                    weight = 1.55f,
                    size = DiscoverTileSize.Hero,
                    corners = TileCorners(32.dp, 12.dp, 12.dp, 28.dp),
                    indexOffset = 0,
                ),
                ColumnSpec.Stack(
                    weight = 1f,
                    tiles = listOf(
                        StackTile(
                            size = DiscoverTileSize.Small,
                            corners = TileCorners(12.dp, 24.dp, 12.dp, 12.dp),
                            indexOffset = 1,
                        ),
                        StackTile(
                            size = DiscoverTileSize.Small,
                            corners = TileCorners(12.dp, 12.dp, 24.dp, 12.dp),
                            indexOffset = 2,
                        ),
                    ),
                ),
            ),
        ),
    ),
)

/** Three-tile row, then a full-width medium below. */
private fun fourTiles() = LayoutSpec(
    rows = listOf(
        threeTiles().rows.first(),
        RowSpec(
            height = 132.dp,
            columns = listOf(
                ColumnSpec.Single(
                    weight = 1f,
                    size = DiscoverTileSize.Medium,
                    corners = TileCorners(12.dp, 28.dp, 28.dp, 12.dp),
                    indexOffset = 3,
                ),
            ),
        ),
    ),
)

/** Three-tile row, then a medium pair (55/45) below. */
private fun fiveTiles() = LayoutSpec(
    rows = listOf(
        threeTiles().rows.first(),
        RowSpec(
            height = 132.dp,
            columns = listOf(
                ColumnSpec.Single(
                    weight = 1.15f,
                    size = DiscoverTileSize.Medium,
                    corners = TileCorners(12.dp, 12.dp, 12.dp, 28.dp),
                    indexOffset = 3,
                ),
                ColumnSpec.Single(
                    weight = 0.95f,
                    size = DiscoverTileSize.Medium,
                    corners = TileCorners(28.dp, 12.dp, 12.dp, 12.dp),
                    indexOffset = 4,
                ),
            ),
        ),
    ),
)

/** Hero+stack row, then a *mirrored* stack+hero row — alternates the focal side. */
private fun sixTiles() = LayoutSpec(
    rows = listOf(
        threeTiles().rows.first(),
        RowSpec(
            height = 224.dp,
            columns = listOf(
                ColumnSpec.Stack(
                    weight = 1f,
                    tiles = listOf(
                        StackTile(
                            size = DiscoverTileSize.Small,
                            corners = TileCorners(28.dp, 12.dp, 12.dp, 12.dp),
                            indexOffset = 3,
                        ),
                        StackTile(
                            size = DiscoverTileSize.Small,
                            corners = TileCorners(12.dp, 12.dp, 12.dp, 24.dp),
                            indexOffset = 4,
                        ),
                    ),
                ),
                ColumnSpec.Single(
                    weight = 1.55f,
                    size = DiscoverTileSize.Hero,
                    corners = TileCorners(12.dp, 28.dp, 32.dp, 12.dp),
                    indexOffset = 5,
                ),
            ),
        ),
    ),
)

/** Reverse the column order of every row + horizontally swap each shape's corners. */
private fun LayoutSpec.mirrored(): LayoutSpec = LayoutSpec(
    rows = rows.map { row ->
        row.copy(
            columns = row.columns.reversed().map { col -> col.mirrored() },
        )
    },
)

private fun ColumnSpec.mirrored(): ColumnSpec = when (this) {
    is ColumnSpec.Single -> copy(corners = corners.swapHorizontal())
    is ColumnSpec.Stack -> copy(
        tiles = tiles.map { tile -> tile.copy(corners = tile.corners.swapHorizontal()) },
    )
}

private fun TileCorners.swapHorizontal(): TileCorners = TileCorners(
    topStart = topEnd,
    topEnd = topStart,
    bottomEnd = bottomStart,
    bottomStart = bottomEnd,
)

/** Spillover: append additional full-width mediums for any tiles beyond 6. */
private fun LayoutSpec.extendedWith(extraTiles: Int): LayoutSpec {
    if (extraTiles <= 0) return this
    val baseTileCount = 6
    val extras = (0 until extraTiles).map { i ->
        RowSpec(
            height = 132.dp,
            columns = listOf(
                ColumnSpec.Single(
                    weight = 1f,
                    size = DiscoverTileSize.Medium,
                    corners = if (i % 2 == 0) {
                        TileCorners(28.dp, 12.dp, 12.dp, 28.dp)
                    } else {
                        TileCorners(12.dp, 28.dp, 28.dp, 12.dp)
                    },
                    indexOffset = baseTileCount + i,
                ),
            ),
        )
    }
    return LayoutSpec(rows = rows + extras)
}

// ─── Tonal palette discipline ────────────────────────────────────────────────

/**
 * Ensures adjacent tiles never share the same tonal variant by rotating the
 * palette and swapping when a duplicate is detected.
 *
 * If the palette is shorter than the count, it wraps around.
 */
private fun ensureNoAdjacentRepeats(
    palette: List<DiscoverTonal>,
    count: Int,
): List<DiscoverTonal> {
    if (palette.isEmpty()) return emptyList()
    val pool = palette.toMutableList()
    while (pool.size < count) pool.addAll(palette)

    val result = mutableListOf<DiscoverTonal>()
    for (i in 0 until count) {
        var candidate = pool[i]
        if (i > 0 && result[i - 1] == candidate) {
            // Find a swap partner further down the pool, if any
            val swapIdx = (i + 1 until pool.size).firstOrNull { pool[it] != candidate }
            if (swapIdx != null) {
                val tmp = pool[swapIdx]
                pool[swapIdx] = candidate
                pool[i] = tmp
                candidate = tmp
            }
        }
        result += candidate
    }
    return result
}
