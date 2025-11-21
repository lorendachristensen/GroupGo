package com.lorenda.groupgo.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.lorenda.groupgo.data.UserProfile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    user: FirebaseUser?,
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    profile: UserProfile? = null,
    onUpdateProfile: (UserProfile) -> Unit = {},
    isLoading: Boolean = false
) {
    val resolvedProfile = profile ?: UserProfile(uid = user?.uid ?: "")

    var firstName by remember(profile) { mutableStateOf(resolvedProfile.firstName) }
    var lastName by remember(profile) { mutableStateOf(resolvedProfile.lastName) }
    var displayName by remember(profile) { mutableStateOf(resolvedProfile.displayName) }
    var profilePic by remember(profile) { mutableStateOf(resolvedProfile.profilePic) }
    var shortBio by remember(profile) { mutableStateOf(resolvedProfile.shortBio) }
    var homeAirport by remember(profile) { mutableStateOf(resolvedProfile.homeAirport) }
    var passportId by remember(profile) { mutableStateOf(resolvedProfile.passportId) }
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                onUpdateProfile(
                                    resolvedProfile.copy(
                                        firstName = firstName,
                                        lastName = lastName,
                                        displayName = displayName.ifBlank { "$firstName $lastName".trim() },
                                        profilePic = profilePic,
                                        shortBio = shortBio,
                                        homeAirport = homeAirport,
                                        passportId = passportId
                                    )
                                )
                                isEditing = false
                            },
                            enabled = !isLoading && firstName.isNotBlank() && lastName.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Avatar
            Card(
                modifier = Modifier.size(120.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Display Name Section
            if (isEditing) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("Enter your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = profilePic,
                    onValueChange = { profilePic = it },
                    label = { Text("Profile Picture URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = shortBio,
                    onValueChange = { shortBio = it },
                    label = { Text("Short Bio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = homeAirport,
                    onValueChange = { homeAirport = it },
                    label = { Text("Home Airport") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = passportId,
                    onValueChange = { passportId = it },
                    label = { Text("Passport ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            firstName = resolvedProfile.firstName
                            lastName = resolvedProfile.lastName
                            displayName = resolvedProfile.displayName
                            profilePic = resolvedProfile.profilePic
                            shortBio = resolvedProfile.shortBio
                            homeAirport = resolvedProfile.homeAirport
                            passportId = resolvedProfile.passportId
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onUpdateProfile(
                                resolvedProfile.copy(
                                    firstName = firstName,
                                    lastName = lastName,
                                    displayName = displayName.ifBlank { "$firstName $lastName".trim() },
                                    profilePic = profilePic,
                                    shortBio = shortBio,
                                    homeAirport = homeAirport,
                                    passportId = passportId
                                )
                            )
                            isEditing = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && firstName.isNotBlank() && lastName.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            } else {
                Text(
                    text = displayName.ifEmpty { resolvedProfile.displayName.ifEmpty { "No name set" } },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (displayName.isEmpty()) {
                    TextButton(onClick = { isEditing = true }) {
                        Text("Add your name")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Account Information Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Account Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                user?.email ?: "Not available",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ProfileRow(label = "First Name", value = resolvedProfile.firstName.ifBlank { "Not set" })
                    ProfileRow(label = "Last Name", value = resolvedProfile.lastName.ifBlank { "Not set" })
                    ProfileRow(label = "Display Name", value = resolvedProfile.displayName.ifBlank { "Not set" })
                    ProfileRow(label = "Profile Picture", value = resolvedProfile.profilePic.ifBlank { "Not set" })
                    ProfileRow(label = "Short Bio", value = resolvedProfile.shortBio.ifBlank { "Not set" })
                    ProfileRow(label = "Home Airport", value = resolvedProfile.homeAirport.ifBlank { "Not set" })
                    ProfileRow(label = "Passport ID", value = resolvedProfile.passportId.ifBlank { "Not set" })

                    Spacer(modifier = Modifier.height(16.dp))

                    // Member Since
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Member Since",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val creationTime = user?.metadata?.creationTimestamp
                            val dateText = if (creationTime != null) {
                                SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(creationTime))
                            } else {
                                "Unknown"
                            }
                            Text(
                                dateText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sign Out Button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out?") },
                text = { Text("Are you sure you want to sign out?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogoutClick()
                        }
                    ) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen(user = null, profile = UserProfile(firstName = "Ava", lastName = "Chen"))
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

