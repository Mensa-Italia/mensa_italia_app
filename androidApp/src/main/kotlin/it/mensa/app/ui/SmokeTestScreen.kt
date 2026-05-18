package it.mensa.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.components.PrimaryButton
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.AuthState
import it.mensa.shared.repository.EventsRepository
import it.mensa.shared.repository.NotificationsRepository
import it.mensa.shared.repository.SearchRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokeTestScreen(
    auth: AuthRepository = koinInject(),
    eventsRepo: EventsRepository = koinInject(),
    notifRepo: NotificationsRepository = koinInject(),
    searchRepo: SearchRepository = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val authState by auth.authState.collectAsState()
    var initDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!initDone) {
            auth.init()
            initDone = true
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Mensa SDK smoke") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val s = authState) {
                AuthState.Unknown -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                AuthState.Anonymous -> LoginForm(onLogin = { email, pwd ->
                    scope.launch { auth.login(email, pwd) }
                })
                is AuthState.Authenticated -> AuthedView(eventsRepo, notifRepo, searchRepo, onLogout = {
                    scope.launch { auth.logout() }
                })
            }
        }
    }
}

@Composable
private fun LoginForm(onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        Spacer(Modifier.height(16.dp))
        PrimaryButton(text = "Login", onClick = { onLogin(email, password) })
    }
}

@Composable
private fun AuthedView(
    eventsRepo: EventsRepository,
    notifRepo: NotificationsRepository,
    searchRepo: SearchRepository,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val events by eventsRepo.observeAll().collectAsState(initial = emptyList())
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var realtimeActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchState by searchRepo.state.collectAsState(initial = SearchRepository.State.Idle)

    LaunchedEffect(Unit) {
        refreshing = true
        runCatching { eventsRepo.refresh() }.onFailure { error = it.message }
        refreshing = false
        notifRepo.observeRealtime(scope)
        realtimeActive = true
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("● realtime ${if (realtimeActive) "ON" else "..."}")
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onLogout) { Text("Logout") }
        }

        // Master search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                searchRepo.update(it)
            },
            label = { Text("Master search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        when (val s = searchState) {
            is SearchRepository.State.Loading ->
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
            is SearchRepository.State.Success -> {
                val summary = s.response.results.entries
                    .filter { it.value.isNotEmpty() }
                    .joinToString(", ") { "${it.value.size} ${it.key}" }
                if (summary.isNotEmpty()) {
                    Text(summary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
            is SearchRepository.State.Error ->
                Text("Search error: ${s.cause.message}", color = Color.Red, style = MaterialTheme.typography.bodySmall)
            is SearchRepository.State.Idle -> { /* nothing */ }
        }

        if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        LazyColumn(Modifier.fillMaxSize()) {
            items(events) { e ->
                ListItem(headlineContent = { Text(e.name) }, supportingContent = { Text(e.description.take(80)) })
                HorizontalDivider()
            }
        }
    }
}
