package com.sage.app.ui.publicapi

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sage.app.publicapi.PublicApiRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicApiScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val vm: PublicApiViewModel = viewModel(factory = PublicApiViewModel.Factory(PublicApiRepository()))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Scaffold(modifier = modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text("Public APIs") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
            OutlinedTextField(value = state.query, onValueChange = vm::query, singleLine = true, label = { Text("Search APIs") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { TextButton(onClick = vm::search) { Text("Search") } })
            Spacer(Modifier.height(8.dp))
            if (state.categories.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(state.category ?: "All categories") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("All categories") }, onClick = { expanded = false; vm.category(null) })
                        state.categories.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { expanded = false; vm.category(cat) }) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(8.dp)) }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.entries, key = { it.api }) { api ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(api.api, style = MaterialTheme.typography.titleMedium); Icon(Icons.Default.Public, null) }
                            Text(api.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                            Text("${api.category ?: "Uncategorized"} • ${api.auth ?: "No auth"} • HTTPS ${if (api.https) "yes" else "no"}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
                            if (api.link.startsWith("https://")) TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(api.link))) }) { Text("Documentation") }
                        }
                    }
                }
            }
        }
    }
}
