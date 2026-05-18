package it.mensa.app.services.push

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * PushPermissionRequester — Compose helper for POST_NOTIFICATIONS permission (Android 13+).
 *
 * Usage:
 * ```kotlin
 * PushPermissionRequester(
 *     onGranted = { /* token registration, etc. */ },
 *     onDenied = { /* show rationale or skip */ },
 * )
 * ```
 *
 * On Android < 13 the permission is automatically considered granted and
 * [onGranted] is called immediately.
 */
@Composable
fun PushPermissionRequester(
    requestOnLaunch: Boolean = true,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {},
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) { onGranted() }
        return
    }

    var permissionRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) onGranted() else onDenied()
    }

    if (requestOnLaunch && !permissionRequested) {
        LaunchedEffect(Unit) {
            permissionRequested = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
