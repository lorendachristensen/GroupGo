package com.lorenda.groupgo.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lorenda.groupgo.data.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutMeScreen(
    profile: UserProfile,
    onBackClick: () -> Unit = {},
    onSave: (UserProfile) -> Unit = {},
    isLoading: Boolean = false
) {
    var displayName by remember(profile) { mutableStateOf(profile.displayName) }
    var shortBio by remember(profile) { mutableStateOf(profile.shortBio) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Me") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(profile.copy(displayName = displayName, shortBio = shortBio)) },
                        enabled = !isLoading
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Display Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            Text("Short Bio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = shortBio,
                onValueChange = { shortBio = it },
                label = { Text("Short Bio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                enabled = !isLoading
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutMeScreenPreview() {
    MaterialTheme {
        AboutMeScreen(
            profile = UserProfile(displayName = "Ava", shortBio = "Traveler"),
            onBackClick = {},
            onSave = {}
        )
    }
}
