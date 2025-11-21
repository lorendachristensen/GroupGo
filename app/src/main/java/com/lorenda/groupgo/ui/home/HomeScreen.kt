package com.lorenda.groupgo.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lorenda.groupgo.data.Trip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    userEmail: String,
    organizedTrips: List<Trip> = emptyList(),
    participatingTrips: List<Trip> = emptyList(),
    onCreateTripClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    pendingInvitesCount: Int = 0,
    onInvitesHubClick: () -> Unit = {},
    onTripClick: (Trip) -> Unit = {},
    onInviteClick: (Trip) -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onDeleteTrip: (String) -> Unit = {}
) {
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GroupGo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onInvitesHubClick) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Invitations",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateTripClick,
                icon = { Icon(Icons.Default.Add, contentDescription = "Create Trip") },
                text = { Text("New Trip") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome back!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = userEmail,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (pendingInvitesCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    onClick = onInvitesHubClick
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "You have $pendingInvitesCount invite${if (pendingInvitesCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Tap to review and accept/decline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Organized by you",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (organizedTrips.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text("No trips yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Start planning your next adventure!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onCreateTripClick) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Your First Trip")
                            }
                        }
                    }
                } else {
                    organizedTrips.forEach { trip ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onTripClick(trip) },
                                    onLongClick = { tripToDelete = trip }
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(trip.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Destination: ${trip.destination}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text("Budget: $${trip.budget}/person", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("People: ${trip.numberOfPeople}", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onInviteClick(trip) }) {
                                        Text("Invite friends")
                                    }
                                }
                            }
                        }
                    }
                }

                if (participatingTrips.isNotEmpty()) {
                    Text(
                        text = "Trips you're joining",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    participatingTrips.forEach { trip ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onTripClick(trip) },
                                    onLongClick = { tripToDelete = trip }
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(trip.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Destination: ${trip.destination}", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text("Budget: $${trip.budget}/person", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("People: ${trip.numberOfPeople}", style = MaterialTheme.typography.bodySmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onInviteClick(trip) }) {
                                        Text("Invite friends")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            tripToDelete?.let { trip ->
                AlertDialog(
                    onDismissRequest = { tripToDelete = null },
                    title = { Text("Delete Trip?") },
                    text = { Text("Are you sure you want to delete '${trip.name}'? This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeleteTrip(trip.id)
                                tripToDelete = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { tripToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        userEmail = "test@example.com",
        organizedTrips = listOf(
            Trip(
                id = "1",
                name = "Weekend Getaway",
                destination = "Nashville",
                budget = "400",
                numberOfPeople = "3"
            )
        ),
        participatingTrips = emptyList()
    )
}
