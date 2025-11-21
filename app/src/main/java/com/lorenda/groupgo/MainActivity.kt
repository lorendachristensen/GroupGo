package com.lorenda.groupgo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lorenda.groupgo.ui.auth.LoginScreen
import com.lorenda.groupgo.ui.auth.SignUpScreen
import com.lorenda.groupgo.ui.theme.GroupGoTheme
import com.lorenda.groupgo.ui.home.HomeScreen
import com.lorenda.groupgo.ui.trips.ChooseTripApproachScreen
import com.lorenda.groupgo.ui.trips.CreateTripScreen
import com.lorenda.groupgo.ui.trips.EditTripScreen
import com.lorenda.groupgo.ui.trips.ExploreTripScreen
import com.lorenda.groupgo.ui.trips.TripDetailsScreen
import com.lorenda.groupgo.ui.trips.ParticipantDisplay
import com.lorenda.groupgo.ui.trips.TravelSurveyScreen
import com.lorenda.groupgo.ui.profile.ProfileScreen
import com.lorenda.groupgo.data.TripRepository
import com.lorenda.groupgo.data.Trip
import com.lorenda.groupgo.data.ProfileRepository
import com.lorenda.groupgo.data.UserProfile
import com.lorenda.groupgo.data.InvitationRepository
import com.lorenda.groupgo.data.Invitation
import com.lorenda.groupgo.ui.profile.AboutMeScreen
import com.lorenda.groupgo.ui.profile.TravelInfoScreen
import com.lorenda.groupgo.ui.profile.PaymentCardsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GroupGoTheme {
                GroupGoApp()
            }
        }
    }
}

private suspend fun fetchParticipantsDisplay(
    trip: Trip,
    invitations: List<Invitation>,
    profileRepository: ProfileRepository
): List<ParticipantDisplay> {
    val uidEmailMap = mutableMapOf<String, String>()
    trip.participants.forEachIndexed { index, uid ->
        trip.participantsEmails.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { email ->
            uidEmailMap[uid] = email
        }
    }
    trip.participants.forEach { uid ->
        if (uid == trip.createdBy && trip.createdByEmail.isNotBlank()) {
            uidEmailMap.putIfAbsent(uid, trip.createdByEmail)
        }
    }
    invitations.filter { it.status == "accepted" }.forEach { invite ->
        val uid = invite.acceptedByUid
        if (uid.isNotBlank() && invite.invitedEmail.isNotBlank()) {
            uidEmailMap.putIfAbsent(uid, invite.invitedEmail)
        }
    }

    return trip.participants.map { uid ->
        val email = uidEmailMap[uid] ?: "Unknown"
        val profile = profileRepository.getProfile(uid).getOrNull()
        val nameFromProfile = profile?.displayName?.takeIf { it.isNotBlank() }
        val nameFromInvite = invitations.firstOrNull { it.acceptedByUid == uid }
            ?.acceptedByDisplayName?.takeIf { it.isNotBlank() }
        val name = nameFromProfile ?: nameFromInvite ?: email.ifBlank { "Unknown" }
        ParticipantDisplay(name = name, email = email, status = "Participant", uid = uid)
    }
}
@Composable
fun GroupGoApp() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val context = LocalContext.current
    val currentUserId = authViewModel.auth.currentUser?.uid.orEmpty()

    // Trip repository and trips list
    val tripRepository = remember { TripRepository() }
    val trips by tripRepository.getUserTrips().collectAsState(initial = emptyList())
    val profileRepository = remember { ProfileRepository() }
    val invitationRepository = remember { InvitationRepository() }
    val pendingInvites by remember(isLoggedIn) {
        if (isLoggedIn) invitationRepository.getPendingInvitations() else flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isProfileLoading by remember { mutableStateOf(false) }

    // Track which screen to show
    var showSignUp by remember { mutableStateOf(false) }
    var showCreateTrip by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }  // ONLY ADDITION: Profile state
    var showEditTrip by remember { mutableStateOf(false) }
    var showTripDetails by remember { mutableStateOf(false) }
    var showChooseTripApproach by remember { mutableStateOf(false) }
    var showExploreTrip by remember { mutableStateOf(false) }
    var exploreIsSubmitting by remember { mutableStateOf(false) }
    var showTravelSurvey by remember { mutableStateOf(false) }
    var surveyTrip by remember { mutableStateOf<Trip?>(null) }
    var showAboutMe by remember { mutableStateOf(false) }
    var showTravelInfo by remember { mutableStateOf(false) }
    var showPaymentCards by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var tripDetails by remember { mutableStateOf<Trip?>(null) }

    var tripDetailsInvites by remember { mutableStateOf<List<Invitation>>(emptyList()) }
    var tripDetailsParticipants by remember { mutableStateOf<List<ParticipantDisplay>>(emptyList()) }

    var editInvites by remember { mutableStateOf<List<Invitation>>(emptyList()) }
    var editParticipants by remember { mutableStateOf<List<ParticipantDisplay>>(emptyList()) }

    // Coroutine scope for async operations
    val scope = rememberCoroutineScope()

    // Show messages based on auth state
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetState()
                // Reset to login screen after successful signup
                if (state.message.contains("created")) {
                    showSignUp = false
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(showProfile, isLoggedIn) {
        if (showProfile && isLoggedIn) {
            val uid = authViewModel.auth.currentUser?.uid
            if (uid != null) {
                isProfileLoading = true
                val result = profileRepository.getProfile(uid)
                userProfile = result.getOrNull() ?: UserProfile(uid = uid)
                isProfileLoading = false
            }
        } else {
            userProfile = null
        }
    }

    LaunchedEffect(tripDetails?.id, isLoggedIn) {
        tripDetailsInvites = emptyList()
        tripDetailsParticipants = emptyList()
        val trip = tripDetails
        if (trip != null && isLoggedIn) {
            // Collect invitations for this trip
            launch {
                invitationRepository.getInvitationsForTrip(trip.id).collectLatest { invites ->
                    tripDetailsInvites = invites
                    tripDetailsParticipants = fetchParticipantsDisplay(trip, invites, profileRepository)
                }
            }
        }
    }

    LaunchedEffect(tripToEdit?.id, isLoggedIn) {
        editInvites = emptyList()
        editParticipants = emptyList()
        val trip = tripToEdit
        if (trip != null && isLoggedIn) {
            launch {
                invitationRepository.getInvitationsForTrip(trip.id).collectLatest { invites ->
                    editInvites = invites
                    editParticipants = fetchParticipantsDisplay(trip, invites, profileRepository)
                }
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when {
            // ONLY ADDITION: Profile screen case
            showProfile && isLoggedIn -> {
                ProfileScreen(
                    user = authViewModel.auth.currentUser,
                    onBackClick = {
                        showProfile = false
                    },
                    onLogoutClick = {
                        authViewModel.signOut()
                        showProfile = false
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    },
                    profile = userProfile,
                    onUpdateProfile = { updatedProfile ->
                        scope.launch {
                            val uid = authViewModel.auth.currentUser?.uid
                            if (uid != null) {
                                isProfileLoading = true
                                val resolvedProfile = updatedProfile.copy(uid = uid)
                                val result = profileRepository.upsertProfile(resolvedProfile)
                                if (result.isSuccess) {
                                    userProfile = resolvedProfile
                                    // Keep Firebase display name in sync for other flows
                                    authViewModel.updateProfile(resolvedProfile.displayName)
                                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error updating profile: ${result.exceptionOrNull()?.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                isProfileLoading = false
                            }
                        }
                    },
                    onNavigateAbout = {
                        showProfile = false
                        showAboutMe = true
                    },
                    onNavigateTravel = {
                        showProfile = false
                        showTravelInfo = true
                    },
                    onNavigatePayments = {
                        showProfile = false
                        showPaymentCards = true
                    },
                    onChangePhoto = {
                        Toast.makeText(context, "Photo upload coming soon", Toast.LENGTH_SHORT).show()
                    },
                    isLoading = isProfileLoading || authState is AuthState.Loading
                )
            }
            showAboutMe && isLoggedIn -> {
                val profile = userProfile ?: UserProfile(uid = authViewModel.auth.currentUser?.uid ?: "")
                AboutMeScreen(
                    profile = profile,
                    onBackClick = {
                        showAboutMe = false
                        showProfile = true
                    },
                    onSave = { updated ->
                        scope.launch {
                            val result = profileRepository.upsertProfile(updated)
                            if (result.isSuccess) {
                                userProfile = updated
                                authViewModel.updateProfile(updated.displayName)
                                Toast.makeText(context, "About updated", Toast.LENGTH_SHORT).show()
                                showAboutMe = false
                                showProfile = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error updating about: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
            showTravelInfo && isLoggedIn -> {
                val profile = userProfile ?: UserProfile(uid = authViewModel.auth.currentUser?.uid ?: "")
                TravelInfoScreen(
                    profile = profile,
                    onBackClick = {
                        showTravelInfo = false
                        showProfile = true
                    },
                    onSave = { updated ->
                        scope.launch {
                            val result = profileRepository.upsertProfile(updated)
                            if (result.isSuccess) {
                                userProfile = updated
                                Toast.makeText(context, "Travel info updated", Toast.LENGTH_SHORT).show()
                                showTravelInfo = false
                                showProfile = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error updating travel info: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
            showPaymentCards && isLoggedIn -> {
                PaymentCardsScreen(
                    onBackClick = {
                        showPaymentCards = false
                        showProfile = true
                    },
                    uid = authViewModel.auth.currentUser?.uid.orEmpty(),
                    email = authViewModel.auth.currentUser?.email.orEmpty()
                )
            }
            showTravelSurvey && isLoggedIn && surveyTrip != null -> {
                TravelSurveyScreen(
                    tripName = surveyTrip?.name.orEmpty(),
                    onBackClick = {
                        showTravelSurvey = false
                    }
                )
            }
            showTripDetails && isLoggedIn && tripDetails != null -> {
                TripDetailsScreen(
                    trip = tripDetails!!,
                    participants = tripDetailsParticipants,
                    invitations = tripDetailsInvites,
                    onBackClick = {
                        showTripDetails = false
                        tripDetails = null
                    },
                    onEditClick = {
                        tripToEdit = tripDetails
                        showTripDetails = false
                        showEditTrip = true
                    },
                    currentUserId = currentUserId,
                    onSurveyClick = {
                        surveyTrip = tripDetails
                        showTravelSurvey = true
                    }
                )
            }
            showChooseTripApproach && isLoggedIn -> {
                ChooseTripApproachScreen(
                    onBackClick = {
                        showChooseTripApproach = false
                    },
                    onPlanSpecificClick = {
                        showChooseTripApproach = false
                        showCreateTrip = true
                    },
                    onExploreClick = {
                        showChooseTripApproach = false
                        showExploreTrip = true
                    }
                )
            }
            showCreateTrip && isLoggedIn -> {
                CreateTripScreen(
                    onBackClick = {
                        showCreateTrip = false
                        showChooseTripApproach = false
                    },

                    onCreateClick = { name, destination, budget, people, inviteEmail ->
                        scope.launch {
                            val result = tripRepository.createTrip(
                                name = name,
                                destination = destination,
                                startDate = "TBD",  // We'll improve date handling later
                                endDate = "TBD",
                                budget = budget,
                                numberOfPeople = people
                            )

                            if (result.isSuccess) {
                                val tripId = result.getOrNull()
                                if (!inviteEmail.isNullOrBlank() && tripId != null) {
                                    val inviteResult = invitationRepository.sendInvitation(
                                        tripId = tripId,
                                        tripName = name,
                                        invitedEmail = inviteEmail.trim()
                                    )
                                    if (inviteResult.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "Invitation sent to $inviteEmail",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Trip created but invite failed: ${inviteResult.exceptionOrNull()?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    "Trip '$name' created successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                                showCreateTrip = false
                                showChooseTripApproach = false
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error creating trip: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
            showExploreTrip && isLoggedIn -> {
                ExploreTripScreen(
                    onBackClick = {
                        showExploreTrip = false
                        showChooseTripApproach = true
                    },
                    onCreateClick = { name, invitees ->
                        if (exploreIsSubmitting) return@ExploreTripScreen
                        exploreIsSubmitting = true
                        scope.launch {
                            try {
                                val createResult = tripRepository.createExploratoryTrip(name)
                                val tripId = createResult.getOrNull()
                                val success = createResult.isSuccess && tripId != null
                                if (success && tripId != null) {
                                    val failedInvite = if (invitees.isNotEmpty()) {
                                        invitees.firstNotNullOfOrNull { email ->
                                            val inviteResult = invitationRepository.sendInvitation(
                                                tripId = tripId,
                                                tripName = name,
                                                invitedEmail = email
                                            )
                                            if (inviteResult.isFailure) inviteResult else null
                                        }
                                    } else {
                                        null
                                    }

                                    failedInvite?.let { failure ->
                                        Toast.makeText(
                                            context,
                                            "Trip created, but some invites failed: ${failure.exceptionOrNull()?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } ?: Toast.makeText(
                                        context,
                                        if (invitees.isEmpty()) "Exploratory trip '$name' created" else "Trip and invites sent",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    showExploreTrip = false
                                    showChooseTripApproach = false
                                    return@launch
                                }

                                Toast.makeText(
                                    context,
                                    "Error creating trip: ${createResult.exceptionOrNull()?.message ?: "Unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Error creating trip: ${e.message ?: "Unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                exploreIsSubmitting = false
                            }
                        }
                    },
                    isSubmitting = exploreIsSubmitting
                )
            }
            showEditTrip && isLoggedIn && tripToEdit != null -> {
                val trip = tripToEdit!!
                EditTripScreen(
                    trip = trip,
                    onBackClick = {
                        showEditTrip = false
                        tripToEdit = null
                    },
                    onSaveClick = { name, destination, budget, people, startDate, endDate ->
                        scope.launch {
                            val result = tripRepository.updateTrip(
                                tripId = trip.id,
                                name = name,
                                destination = destination,
                                budget = budget,
                                numberOfPeople = people,
                                startDate = startDate,
                                endDate = endDate
                            )
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Trip updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                showEditTrip = false
                                tripToEdit = null
                                tripDetails = trip.copy(
                                    name = name,
                                    destination = destination,
                                    budget = budget,
                                    numberOfPeople = people,
                                    startDate = startDate,
                                    endDate = endDate
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error updating trip: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onSendInvite = { tripId, tripName, inviteEmail ->
                        scope.launch {
                            val inviteResult = invitationRepository.sendInvitation(
                                tripId = tripId,
                                tripName = tripName,
                                invitedEmail = inviteEmail
                            )
                            if (inviteResult.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Invitation sent to $inviteEmail",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error sending invite: ${inviteResult.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onResendInvite = { tripId, tripName, inviteEmail ->
                        scope.launch {
                            val inviteResult = invitationRepository.sendInvitation(
                                tripId = tripId,
                                tripName = tripName,
                                invitedEmail = inviteEmail
                            )
                            if (inviteResult.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Resent invite to $inviteEmail",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error resending invite: ${inviteResult.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    participants = editParticipants,
                    invites = editInvites
                )
            }

            isLoggedIn -> {
                HomeScreen(
                    userEmail = authViewModel.auth.currentUser?.email ?: "User",
                    trips = trips,  // Pass the trips list
                    onCreateTripClick = {
                        showChooseTripApproach = true
                    },
                    onProfileClick = {  // ONLY ADDITION: Profile click handler
                        showProfile = true
                    },
                    onLogoutClick = {
                        authViewModel.signOut()
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteTrip = { tripId ->
                        scope.launch {
                            val result = tripRepository.deleteTrip(tripId)
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Trip deleted successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error deleting trip: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onEditTrip = { trip ->
                        tripToEdit = trip
                        showEditTrip = true
                    },
                    onTripClick = { trip ->
                        tripDetails = trip
                        showTripDetails = true
                    },
                    invites = pendingInvites,
                    onAcceptInvite = { invitation ->
                        scope.launch {
                            val result = invitationRepository.acceptInvitation(invitation.id, invitation.tripId)
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Joined trip '${invitation.tripName}'",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error accepting invite: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onDeclineInvite = { invitation ->
                        scope.launch {
                            val result = invitationRepository.declineInvitation(invitation.id)
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Invitation declined",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error declining invite: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
            showSignUp -> {
                SignUpScreen(
                    modifier = Modifier.padding(innerPadding),
                    onSignUpClick = { email, password, firstName, lastName, displayName, profilePic, shortBio, homeAirport, passportId ->
                        authViewModel.signUp(
                            email = email,
                            password = password,
                            firstName = firstName,
                            lastName = lastName,
                            displayName = displayName,
                            profilePic = profilePic,
                            shortBio = shortBio,
                            homeAirport = homeAirport,
                            passportId = passportId
                        )
                    },
                    onLoginClick = {
                        showSignUp = false
                    },
                    isLoading = authState is AuthState.Loading
                )
            }
            else -> {
                LoginScreen(
                    modifier = Modifier.padding(innerPadding),
                    onLoginClick = { email, password ->
                        authViewModel.login(email, password)
                    },
                    onSignUpClick = {
                        showSignUp = true
                    },
                    isLoading = authState is AuthState.Loading
                )
            }
        }
    }
}
