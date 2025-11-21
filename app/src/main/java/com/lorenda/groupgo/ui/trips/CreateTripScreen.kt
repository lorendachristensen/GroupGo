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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lorenda.groupgo.utils.DatePickerDialog as GroupGoDatePickerDialog
import com.lorenda.groupgo.ui.common.ScreenDebugLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    onBackClick: () -> Unit = {},
    onCreateClick: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    var tripName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var numberOfPeople by remember { mutableStateOf("") }
    var inviteEmail by remember { mutableStateOf("") }

    // For date pickers - simplified for now
    var startDate by remember { mutableStateOf("Select date") }
    var endDate by remember { mutableStateOf("Select date") }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

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
                title = { Text("Create New Trip") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
            ScreenDebugLabel("CreateTripScreen.kt")
            // Trip Name
            OutlinedTextField(
                value = tripName,
                onValueChange = { tripName = it },
                label = { Text("Trip Name") },
                placeholder = { Text("Summer Beach Trip") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Destination
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Destination") },
                placeholder = { Text("Miami, Florida") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Start Date
                OutlinedCard(
                    onClick = {
                        showStartDatePicker = true
                    },
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

                // End Date
                OutlinedCard(
                    onClick = {
                        showEndDatePicker = true
                    },
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

            // Budget and People Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Budget Per Person
                OutlinedTextField(
                    value = budget,
                    onValueChange = {
                        // Only allow numbers
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            budget = it
                        }
                    },
                    label = { Text("Budget per person") },
                    placeholder = { Text("500") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                // Number of People
                OutlinedTextField(
                    value = numberOfPeople,
                    onValueChange = {
                        // Only allow numbers
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            numberOfPeople = it
                        }
                    },
                    label = { Text("People") },
                    placeholder = { Text("4") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.weight(0.5f),
                    singleLine = true
                )
            }

            // Trip Style Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Trip Style",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "🎒 Adventure • 🏖️ Relaxation • 🎉 Nightlife",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Style preferences coming soon!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Invite by email (optional)
            OutlinedTextField(
                value = inviteEmail,
                onValueChange = { inviteEmail = it },
                label = { Text("Invite by Email (optional)") },
                placeholder = { Text("friend@example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Create Button
            Button(
                onClick = {
                    onCreateClick(tripName, destination, budget, numberOfPeople, inviteEmail)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = tripName.isNotBlank() &&
                        destination.isNotBlank() &&
                        budget.isNotBlank() &&
                        numberOfPeople.isNotBlank() &&
                        startDate != "Select date" &&
                        endDate != "Select date"
            ) {
                Text(
                    "Create Trip",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateTripScreenPreview() {
    MaterialTheme {
        CreateTripScreen()
    }
}
