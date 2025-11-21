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
fun TravelInfoScreen(
    profile: UserProfile,
    onBackClick: () -> Unit = {},
    onSave: (UserProfile) -> Unit = {},
    isLoading: Boolean = false
) {
    var homeAirport by remember(profile) { mutableStateOf(profile.homeAirport) }
    var passportId by remember(profile) { mutableStateOf(profile.passportId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Travel Info") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(profile.copy(homeAirport = homeAirport, passportId = passportId)) },
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
            Text("Home Airport", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = homeAirport,
                onValueChange = { homeAirport = it },
                label = { Text("Home Airport") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
            Text("Passport ID", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = passportId,
                onValueChange = { passportId = it },
                label = { Text("Passport ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TravelInfoScreenPreview() {
    MaterialTheme {
        TravelInfoScreen(
            profile = UserProfile(homeAirport = "SFO", passportId = "12345678"),
            onBackClick = {},
            onSave = {}
        )
    }
}
