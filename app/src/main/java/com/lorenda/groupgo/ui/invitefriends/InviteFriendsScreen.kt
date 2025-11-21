package com.lorenda.groupgo.ui.invitefriends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lorenda.groupgo.data.Invitation
import com.lorenda.groupgo.data.Trip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteFriendsScreen(
    trip: Trip,
    invitations: List<Invitation> = emptyList(),
    participants: List<String> = emptyList(),
    isOrganizer: Boolean = false,
    isSending: Boolean = false,
    onBackClick: () -> Unit = {},
    onSendInvitation: (String) -> Unit = { _ -> },
    onCancelInvitation: (String) -> Unit = { _ -> },
    onRemoveParticipant: (String) -> Unit = { _ -> }
) {
    var email by remember { mutableStateOf("") }
    val isEmailValid = remember(email) { email.contains("@") && email.contains(".") }
    val title = trip.name.ifBlank { "Trip" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invite to $title") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Friend's email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = email.isNotEmpty() && !isEmailValid
            )
            Button(
                onClick = {
                    onSendInvitation(email.trim())
                    email = ""
                },
                enabled = isEmailValid && !isSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSending) "Sending..." else "Send Invite")
            }

            Text("Invitations sent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(invitations) { invite ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(invite.invitedEmail, style = MaterialTheme.typography.bodyLarge)
                            Text("Status: ${invite.status}", style = MaterialTheme.typography.bodySmall)
                            if (isOrganizer && invite.status == "pending") {
                                TextButton(onClick = { onCancelInvitation(invite.id) }) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InviteFriendsPreview() {
    InviteFriendsScreen(trip = Trip(name = "Preview", destination = "Nowhere"))
}
