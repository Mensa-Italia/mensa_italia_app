package it.mensa.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
/** A place the chip offers, with how many items it holds. */
data class PlaceChoice(val name: String, val count: Int)

/**
 * Il filtro per regione o provincia: una chip che apre il menu dei [places],
 * ognuno col suo numero di elementi quando sono caricati ([showCounts]).
 * Selezionata mostra il luogo scelto; [allLabel] lo toglie.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaceFilterChip(
    label: String,
    allLabel: String,
    places: List<PlaceChoice>,
    selected: String?,
    onSelect: (String?) -> Unit,
    showCounts: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val choose: (String?) -> Unit = { place ->
        expanded = false
        onSelect(place)
    }
    Box(modifier) {
        FilterChip(
            // "Regione" -> "Friuli-Venezia Giulia": la larghezza segue il testo.
            modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected ?: label) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            val check: @Composable () -> Unit = { Icon(Icons.Filled.Check, contentDescription = null) }
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                DropdownMenuItem(
                    selected = selected == null,
                    onClick = { choose(null) },
                    text = { Text(allLabel) },
                    shapes = MenuDefaults.itemShape(0, 1),
                    selectedLeadingIcon = check,
                )
            }
            Spacer(Modifier.height(2.dp))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                places.forEachIndexed { index, place ->
                    DropdownMenuItem(
                        selected = selected == place.name,
                        onClick = { choose(place.name) },
                        text = { Text(place.name) },
                        shapes = MenuDefaults.itemShape(index, places.size),
                        selectedLeadingIcon = check,
                        trailingIcon = if (showCounts) {
                            {
                                Text(
                                    place.count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
