package com.lorenda.groupgo.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lorenda.groupgo.data.Invitation
import com.lorenda.groupgo.data.Trip
import com.lorenda.groupgo.ui.common.ScreenDebugLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailsScreen(
    trip: Trip,
    participants: List<ParticipantDisplay> = emptyList(),
    invitations: List<Invitation> = emptyList(),
    onBackClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    currentUserId: String = "",
    onSurveyClick: () -> Unit = {}
) {
    val pendingOrDeclined = invitations.filter { it.status == "pending" || it.status == "declined" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip.name.ifBlank { "Trip Details" }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Trip")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenDebugLabel("TripDetailsScreen.kt")
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = trip.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trip.destination,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Dates: ${trip.startDate} - ${trip.endDate}")
                        Text("Budget: $${trip.budget}/person")
                        Text("People: ${trip.numberOfPeople}")
                    }
                }
            }

            item {
                Text(
                    text = "Participants",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            val resolvedParticipants = if (participants.isEmpty()) {
                listOf(ParticipantDisplay("None yet", "", "Participant"))
            } else {
                participants
            }
            items(resolvedParticipants) { person ->
                ParticipantRow(
                    person = person,
                    isCurrentUser = person.uid.isNotBlank() && person.uid == currentUserId,
                    onSurveyClick = onSurveyClick
                )
            }

            item {
                Text(
                    text = "Invites (Pending / Declined)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (pendingOrDeclined.isEmpty()) {
                item {
                    Text(
                        text = "No pending or declined invites",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(pendingOrDeclined) { invite ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = invite.invitedEmail,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Status: ${invite.status}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantRow(
    person: ParticipantDisplay,
    isCurrentUser: Boolean,
    onSurveyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = person.email,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (isCurrentUser) {
                TextButton(onClick = onSurveyClick) {
                    Text("Travel Survey")
                }
            } else {
                Text(
                    text = person.status.ifBlank { "Survey pending" },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailsPreview() {
    val trip = Trip(
        name = "Weekend Getaway",
        destination = "New York",
        startDate = "Jan 10, 2025",
        endDate = "Jan 12, 2025",
        budget = "300",
        numberOfPeople = "2"
    )
    val participants = listOf(
        ParticipantDisplay("Ava Chen", "ava@example.com", "Participant", uid = "1"),
        ParticipantDisplay("Sam Lee", "sam@example.com", "Participant", uid = "2")
    )
    val invites = listOf(
        Invitation(invitedEmail = "pending@example.com", status = "pending"),
        Invitation(invitedEmail = "declined@example.com", status = "declined")
    )
    TripDetailsScreen(trip, participants, invites, currentUserId = "1")
}
