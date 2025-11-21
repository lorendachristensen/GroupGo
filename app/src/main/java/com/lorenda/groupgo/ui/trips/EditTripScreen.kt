package com.lorenda.groupgo.ui.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lorenda.groupgo.data.Invitation
import com.lorenda.groupgo.data.Trip
import com.lorenda.groupgo.utils.DatePickerDialog as GroupGoDatePickerDialog
import com.lorenda.groupgo.ui.trips.ParticipantDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTripScreen(
    trip: Trip,
    onBackClick: () -> Unit = {},
    onSaveClick: (
        name: String,
        destination: String,
        budget: String,
        numberOfPeople: String,
        startDate: String,
        endDate: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    onSendInvite: (tripId: String, tripName: String, inviteEmail: String) -> Unit = { _, _, _ -> },
    onResendInvite: (tripId: String, tripName: String, inviteEmail: String) -> Unit = { _, _, _ -> },
    participants: List<ParticipantDisplay> = emptyList(),
    invites: List<Invitation> = emptyList()
) {
    var tripName by remember(trip) { mutableStateOf(trip.name) }
    var destination by remember(trip) { mutableStateOf(trip.destination) }
    var budget by remember(trip) { mutableStateOf(trip.budget) }
    var numberOfPeople by remember(trip) { mutableStateOf(trip.numberOfPeople) }
    var startDate by remember(trip) { mutableStateOf(trip.startDate.ifBlank { "Select date" }) }
    var endDate by remember(trip) { mutableStateOf(trip.endDate.ifBlank { "Select date" }) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }

    if (showStartDatePicker) {
        GroupGoDatePickerDialog(
            onDateSelected = { date ->
                startDate = date
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        GroupGoDatePickerDialog(
            onDateSelected = { date ->
                endDate = date
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Trip") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSaveClick(
                                tripName,
                                destination,
                                budget,
                                numberOfPeople,
                                startDate,
                                endDate
                            )
                        },
                        enabled = tripName.isNotBlank() &&
                                destination.isNotBlank() &&
                                budget.isNotBlank() &&
                                numberOfPeople.isNotBlank() &&
                                startDate != "Select date" &&
                                endDate != "Select date"
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
            OutlinedTextField(
                value = tripName,
                onValueChange = { tripName = it },
                label = { Text("Trip Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedCard(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Start Date",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                startDate,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                OutlinedCard(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "End Date",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                endDate,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = budget,
                    onValueChange = {
                        if (it.isEmpty() || it.all(Char::isDigit)) {
                            budget = it
                        }
                    },
                    label = { Text("Budget per person") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = numberOfPeople,
                    onValueChange = {
                        if (it.isEmpty() || it.all(Char::isDigit)) {
                            numberOfPeople = it
                        }
                    },
                    label = { Text("People") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.5f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = inviteEmail,
                onValueChange = { inviteEmail = it },
                label = { Text("Invite by Email (optional)") },
                placeholder = { Text("friend@example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    if (inviteEmail.isNotBlank()) {
                        onSendInvite(trip.id, trip.name, inviteEmail.trim())
                        inviteEmail = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = inviteEmail.isNotBlank()
            ) {
                Text("Send Invite")
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Participants",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            participants.forEach { participant ->
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
                        Text(participant.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(participant.email, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Text(
                text = "Invites (Pending / Declined)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            invites.filter { it.status == "pending" || it.status == "declined" }.forEach { invite ->
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
                        Text(invite.invitedEmail, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Status: ${invite.status}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onResendInvite(trip.id, trip.name, invite.invitedEmail) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Resend Invite")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSaveClick(
                        tripName,
                        destination,
                        budget,
                        numberOfPeople,
                        startDate,
                        endDate
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = tripName.isNotBlank() &&
                        destination.isNotBlank() &&
                        budget.isNotBlank() &&
                        numberOfPeople.isNotBlank() &&
                        startDate != "Select date" &&
                        endDate != "Select date"
            ) {
                Text("Save Changes", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditTripScreenPreview() {
    MaterialTheme {
        EditTripScreen(
            trip = Trip(
                name = "Weekend Getaway",
                destination = "New York",
                startDate = "Jan 10, 2025",
                endDate = "Jan 12, 2025",
                budget = "300",
                numberOfPeople = "2"
            ),
            participants = listOf(
                ParticipantDisplay("Ava Chen", "ava@example.com", "Participant")
            ),
            invites = listOf(
                com.lorenda.groupgo.data.Invitation(invitedEmail = "pending@example.com", status = "pending")
            )
        )
    }
}
