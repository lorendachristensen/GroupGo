package com.lorenda.groupgo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenda.groupgo.data.InvitationRepository
import com.lorenda.groupgo.data.UserRepository
import com.lorenda.groupgo.data.Trip
import com.lorenda.groupgo.data.TripRepository
import com.lorenda.groupgo.network.StripeBackendService
import com.lorenda.groupgo.network.SetupIntentResponse
import com.lorenda.groupgo.network.PaymentMethodSummary
import com.lorenda.groupgo.fetchPaymentMethodsFromBackend
import com.lorenda.groupgo.ui.auth.LoginScreen
import com.lorenda.groupgo.ui.auth.SignUpScreen
import com.lorenda.groupgo.ui.home.HomeScreen
import com.lorenda.groupgo.ui.invitefriends.InviteFriendsScreen
import com.lorenda.groupgo.ui.invitefriends.MyInvitationsScreen
import com.lorenda.groupgo.ui.profile.ProfileScreen
import com.lorenda.groupgo.ui.theme.GroupGoTheme
import com.lorenda.groupgo.ui.trips.CreateTripScreen
import com.lorenda.groupgo.ui.trips.EditTripScreen
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // Keep a single PaymentSheet instance registered before the Activity is STARTED.
    private var paymentSheetResultCallback: (PaymentSheetResult) -> Unit = {}
    private lateinit var paymentSheet: PaymentSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        paymentSheet = PaymentSheet(this) { result ->
            paymentSheetResultCallback(result)
        }

        setContent {
            GroupGoTheme {
                GroupGoApp(
                    paymentSheet = paymentSheet,
                    setPaymentSheetResultCallback = { cb -> paymentSheetResultCallback = cb }
                )
            }
        }
    }
}

@Composable
fun GroupGoApp(
    paymentSheet: PaymentSheet,
    setPaymentSheetResultCallback: ((PaymentSheetResult) -> Unit) -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val context = LocalContext.current

    val tripRepository = remember { TripRepository() }
    val invitationRepository = remember { InvitationRepository() }
    val userRepository = remember { UserRepository() }

    val currentUserId = authViewModel.auth.currentUser?.uid
    val trips by remember(currentUserId) {
        tripRepository.getUserTrips(currentUserId)
    }.collectAsState(initial = emptyList())
    val pendingInvites by remember(isLoggedIn) {
        if (isLoggedIn) invitationRepository.getPendingInvitations() else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val backendBase = "http://10.0.2.2:4242"

    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var tripForInvites by remember { mutableStateOf<Trip?>(null) }
    var isSendingInvite by remember { mutableStateOf(false) }
    var showInvitesInbox by remember { mutableStateOf(false) }
    var linkCardInProgress by remember { mutableStateOf(false) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethodSummary>>(emptyList()) }
    var lastCustomerId by remember { mutableStateOf<String?>(null) }

    var showSignUp by remember { mutableStateOf(false) }
    var showCreateTrip by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as ComponentActivity

    // Provide the PaymentSheet callback to the Activity-owned instance
    LaunchedEffect(Unit) {
        setPaymentSheetResultCallback { result ->
            when (result) {
                is PaymentSheetResult.Completed -> {
                    Toast.makeText(context, "Card linked successfully", Toast.LENGTH_SHORT).show()
                    val uid = authViewModel.auth.currentUser?.uid
                    val email = authViewModel.auth.currentUser?.email
                    scope.launch {
                        val pmsResult = fetchPaymentMethodsFromBackend(
                            backendBase = backendBase,
                            customerId = lastCustomerId,
                            uid = uid,
                            email = email
                        )
                        if (pmsResult.isSuccess) {
                            paymentMethods = pmsResult.getOrDefault(emptyList())
                        }
                    }
                }
                is PaymentSheetResult.Canceled -> {
                    Toast.makeText(context, "Card linking canceled", Toast.LENGTH_SHORT).show()
                }
                is PaymentSheetResult.Failed -> {
                    Toast.makeText(context, "Card linking failed: ${result.error.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
            linkCardInProgress = false
        }
    }
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                authViewModel.resetState()
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

    LaunchedEffect(showProfile, currentUserId) {
        if (showProfile && isLoggedIn) {
            val uid = authViewModel.auth.currentUser?.uid
            val email = authViewModel.auth.currentUser?.email
            val pmsResult = fetchPaymentMethodsFromBackend(
                backendBase = backendBase,
                customerId = lastCustomerId,
                uid = uid,
                email = email
            )
            if (pmsResult.isSuccess) {
                paymentMethods = pmsResult.getOrDefault(emptyList())
            }
        }
    }

    // Refresh payment methods when opening profile
    LaunchedEffect(showProfile, currentUserId) {
        if (showProfile && isLoggedIn) {
            val uid = authViewModel.auth.currentUser?.uid
            val email = authViewModel.auth.currentUser?.email
            val pmsResult = fetchPaymentMethodsFromBackend(
                backendBase = backendBase,
                customerId = lastCustomerId,
                uid = uid,
                email = email
            )
            if (pmsResult.isSuccess) {
                paymentMethods = pmsResult.getOrDefault(emptyList())
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when {
            showInvitesInbox && isLoggedIn -> {
                MyInvitationsScreen(
                    pendingInvitations = pendingInvites,
                    onBackClick = { showInvitesInbox = false },
                    onAcceptInvitation = { invitationId, tripId ->
                        scope.launch {
                            val acceptResult = invitationRepository.acceptInvitation(invitationId, tripId)
                            if (acceptResult.isSuccess) {
                                Toast.makeText(context, "Invitation accepted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${acceptResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onDeclineInvitation = { invitationId ->
                        scope.launch {
                            val declineResult = invitationRepository.declineInvitation(invitationId)
                            if (declineResult.isSuccess) {
                                Toast.makeText(context, "Invitation declined", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error: ${declineResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
            tripForInvites != null && isLoggedIn -> {
                val inviteTrip = tripForInvites!!
                val invitesForTrip by invitationRepository.getInvitationsForTrip(inviteTrip.id)
                    .collectAsState(initial = emptyList())
                val isOrganizer = authViewModel.auth.currentUser?.uid == inviteTrip.createdBy

                InviteFriendsScreen(
                    trip = inviteTrip,
                    invitations = invitesForTrip,
                    participants = inviteTrip.participants,
                    isOrganizer = isOrganizer,
                    isSending = isSendingInvite,
                    onBackClick = { tripForInvites = null },
                    onSendInvitation = { invitedEmail ->
                        scope.launch {
                            isSendingInvite = true
                            val result = invitationRepository.sendInvitation(
                                tripId = inviteTrip.id,
                                tripName = inviteTrip.name,
                                invitedEmail = invitedEmail
                            )
                            isSendingInvite = false
                            if (result.isSuccess) {
                                Toast.makeText(context, "Invitation sent to $invitedEmail", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error sending invite: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onCancelInvitation = { invitationId ->
                        scope.launch {
                            val cancelResult = invitationRepository.cancelInvitation(invitationId)
                            if (cancelResult.isSuccess) {
                                Toast.makeText(context, "Invitation canceled", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error canceling invite: ${cancelResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onRemoveParticipant = { userId ->
                        scope.launch {
                            val removeResult = tripRepository.removeParticipant(inviteTrip.id, userId)
                            if (removeResult.isSuccess) {
                                Toast.makeText(context, "Removed participant", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error removing participant: ${removeResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
            tripToEdit != null && isLoggedIn -> {
                val editingTrip = tripToEdit!!
                val invitesForTrip by invitationRepository.getInvitationsForTrip(editingTrip.id)
                    .collectAsState(initial = emptyList())
                val isOrganizer = authViewModel.auth.currentUser?.uid == editingTrip.createdBy
                val participantNames by userRepository.getUserNames(editingTrip.participants)
                    .collectAsState(initial = emptyMap())
                EditTripScreen(
                    trip = editingTrip,
                    onBackClick = {
                        tripToEdit = null
                    },
                    onUpdateClick = { tripId, name, destination, startDate, endDate, budget, people ->
                        scope.launch {
                            val result = tripRepository.updateTrip(
                                tripId = tripId,
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
                                tripToEdit = null
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error updating trip: ${result.exceptionOrNull()?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onInviteClick = { trip ->
                        tripForInvites = trip
                    },
                    invitations = invitesForTrip,
                    participants = editingTrip.participants,
                    participantNames = participantNames,
                    isOrganizer = isOrganizer,
                    onCancelInvitation = { invitationId ->
                        scope.launch {
                            val cancelResult = invitationRepository.cancelInvitation(invitationId)
                            if (cancelResult.isSuccess) {
                                Toast.makeText(context, "Invitation canceled", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error canceling invite: ${cancelResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onRemoveParticipant = { userId ->
                        scope.launch {
                            val removeResult = tripRepository.removeParticipant(editingTrip.id, userId)
                            if (removeResult.isSuccess) {
                                Toast.makeText(context, "Removed participant", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error removing participant: ${removeResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
            showProfile && isLoggedIn -> {
                ProfileScreen(
                    user = authViewModel.auth.currentUser,
                    onBackClick = {
                        showProfile = false
                    },
                    onLogoutClick = {
                        authViewModel.signOut()
                        tripForInvites = null
                        tripToEdit = null
                        showProfile = false
                        showInvitesInbox = false
                        linkCardInProgress = false
                        lastCustomerId = null
                        paymentMethods = emptyList()
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateProfile = { displayName ->
                        authViewModel.updateProfile(displayName)
                    },
                    onLinkCardClick = {
                        val uid = authViewModel.auth.currentUser?.uid
                        val email = authViewModel.auth.currentUser?.email
                        if (uid.isNullOrBlank() || email.isNullOrBlank()) {
                            Toast.makeText(context, "User info missing", Toast.LENGTH_LONG).show()
                            return@ProfileScreen
                        }
                        if (linkCardInProgress) return@ProfileScreen
                        scope.launch {
                            linkCardInProgress = true
                            val backendUrl = "http://10.0.2.2:4242/stripe/setup-intent"
                            val response = StripeBackendService.createSetupIntent(
                                backendUrl = backendUrl,
                                uid = uid,
                                email = email
                            )
                            if (response.isSuccess) {
                                val data = response.getOrNull()
                                if (data != null) {
                                    lastCustomerId = data.customerId
                                    PaymentConfiguration.init(context, data.publishableKey)
                                    val customerConfig = PaymentSheet.CustomerConfiguration(
                                        id = data.customerId,
                                        ephemeralKeySecret = data.ephemeralKey
                                    )
                                    val configuration = PaymentSheet.Configuration(
                                        merchantDisplayName = "GroupGo",
                                        customer = customerConfig,
                                        allowsDelayedPaymentMethods = false
                                    )
                                    paymentSheet.presentWithSetupIntent(
                                        data.setupIntentClientSecret,
                                        configuration
                                    )
                                    // Refresh cards now (setup intent attaches on completion; we also refresh in callback for safety)
                                    val pmsResult = fetchPaymentMethodsFromBackend(
                                        backendBase = backendBase,
                                        customerId = data.customerId,
                                        uid = uid,
                                        email = email
                                    )
                                    if (pmsResult.isSuccess) {
                                        paymentMethods = pmsResult.getOrDefault(emptyList())
                                    }
                                } else {
                                    linkCardInProgress = false
                                    Toast.makeText(context, "Invalid response from backend", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                linkCardInProgress = false
                                Toast.makeText(
                                    context,
                                    "Error preparing card link: ${response.exceptionOrNull()?.localizedMessage ?: response.exceptionOrNull()?.toString() ?: "unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    paymentMethods = paymentMethods,
                    isLinkingCard = linkCardInProgress,
                    isLoading = authState is AuthState.Loading
                )
            }
            showCreateTrip && isLoggedIn -> {
                CreateTripScreen(
                    onBackClick = {
                        showCreateTrip = false
                    },

                    onCreateClick = { name, destination, startDate, endDate, budget, people, invitees ->
                        scope.launch {
                            val result = tripRepository.createTrip(
                                name = name,
                                destination = destination,
                                startDate = startDate,
                                endDate = endDate,
                                budget = budget,
                                numberOfPeople = people
                            )

                            if (result.isSuccess) {
                                val tripId = result.getOrNull() ?: ""
                                if (tripId.isNotBlank()) {
                                    invitees.forEach { email ->
                                        invitationRepository.sendInvitation(
                                            tripId = tripId,
                                            tripName = name,
                                            invitedEmail = email
                                        )
                                    }
                                }
                                Toast.makeText(
                                    context,
                                    "Trip '$name' created successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                                showCreateTrip = false
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

            isLoggedIn -> {
                val currentUserId = authViewModel.auth.currentUser?.uid
                val organizedTrips = trips.filter { it.createdBy == currentUserId }
                val participatingTrips = trips.filter { it.participants.contains(currentUserId) && it.createdBy != currentUserId }

                HomeScreen(
                    userEmail = authViewModel.auth.currentUser?.email ?: "User",
                    organizedTrips = organizedTrips,
                    participatingTrips = participatingTrips,
                    pendingInvitesCount = pendingInvites.size,
                    onCreateTripClick = {
                        tripToEdit = null
                        tripForInvites = null
                        showCreateTrip = true
                    },
                    onProfileClick = {
                        showProfile = true
                    },
                    onInvitesHubClick = {
                        showInvitesInbox = true
                        tripForInvites = null
                        tripToEdit = null
                        showCreateTrip = false
                    },
                    onLogoutClick = {
                        authViewModel.signOut()
                        tripForInvites = null
                        tripToEdit = null
                        showInvitesInbox = false
                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    },
                    onTripClick = { trip ->
                        showCreateTrip = false
                        tripToEdit = trip
                        tripForInvites = null
                    },
                    onInviteClick = { trip ->
                        tripForInvites = trip
                        showCreateTrip = false
                        tripToEdit = null
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
                    }
                )
            }
            showSignUp -> {
                SignUpScreen(
                    modifier = Modifier.padding(innerPadding),
            onSignUpClick = { firstName, lastName, email, password ->
                authViewModel.signUp(email, password, firstName, lastName)
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
