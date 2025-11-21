# CLAUDE.md - GroupGo Project Guide

**Last Updated:** 2025-11-20
**Version:** 1.0
**Project:** GroupGo - Collaborative Trip Planning Android App

This document provides comprehensive guidance for AI assistants (particularly Claude) working on the GroupGo codebase. It covers architecture, conventions, workflows, and key patterns to follow.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Architecture & Patterns](#architecture--patterns)
5. [Coding Conventions](#coding-conventions)
6. [Development Workflows](#development-workflows)
7. [Firebase Integration](#firebase-integration)
8. [UI/UX Guidelines](#uiux-guidelines)
9. [Testing Strategy](#testing-strategy)
10. [Common Tasks & Examples](#common-tasks--examples)
11. [Troubleshooting](#troubleshooting)

---

## Project Overview

**GroupGo** is a native Android application for collaborative trip planning built with modern Android development practices.

### Key Information
- **Package Name:** `com.lorenda.groupgo`
- **Language:** Kotlin (100% Kotlin codebase)
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36
- **Architecture:** Single Activity with Jetpack Compose
- **Backend:** Firebase (Authentication, Firestore, Realtime Database)

### Current Features
- User authentication (email/password via Firebase Auth)
- Trip creation and management
- User-specific trip filtering
- Real-time trip list updates
- Trip deletion with confirmation

---

## Technology Stack

### Core Technologies
- **Kotlin:** 2.0.21
- **Gradle:** 8.13 (Kotlin DSL)
- **Android Gradle Plugin:** 8.13.1
- **Java Compatibility:** 11

### UI Framework
- **Jetpack Compose:** Material 3 (BOM 2024.09.00)
- **Navigation Compose:** 2.8.5
- **Material Icons Extended:** 1.7.6
- **Coil Image Loading:** 2.7.0

### Architecture Components
- **ViewModel:** Lifecycle-aware state management
- **StateFlow/Flow:** Reactive state handling
- **Coroutines:** 1.9.0 (kotlinx-coroutines-android)
- **Lifecycle Runtime KTX:** 2.6.1
- **ViewModel Compose:** 2.8.7

### Backend & Database
- **Firebase BOM:** 33.6.0
  - `firebase-auth-ktx` - User authentication
  - `firebase-firestore-ktx` - Cloud NoSQL database
  - `firebase-database-ktx` - Real-time data sync

### Testing Libraries
- **JUnit:** 4.13.2
- **AndroidX JUnit:** 1.1.5
- **Espresso:** 3.5.1
- **Compose UI Test:** JUnit4 integration

---

## Project Structure

```
/home/user/GroupGo/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/lorenda/groupgo/
│   │   │   │   ├── ui/                     # UI Layer (Feature-based)
│   │   │   │   │   ├── auth/              # Authentication screens
│   │   │   │   │   │   ├── LoginScreen.kt
│   │   │   │   │   │   └── SignUpScreen.kt
│   │   │   │   │   ├── home/              # Home/dashboard
│   │   │   │   │   │   └── HomeScreen.kt
│   │   │   │   │   ├── trips/             # Trip management
│   │   │   │   │   │   └── CreateTripScreen.kt
│   │   │   │   │   └── theme/             # Design system
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   ├── data/                   # Data Layer
│   │   │   │   │   ├── Trip.kt            # Data models
│   │   │   │   │   └── TripRepository.kt  # Data operations
│   │   │   │   ├── AuthViewModel.kt        # ViewModels
│   │   │   │   └── MainActivity.kt         # App entry point
│   │   │   ├── res/                        # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                           # Unit tests
│   │   └── androidTest/                    # Instrumented tests
│   ├── build.gradle.kts                    # App-level build config
│   ├── proguard-rules.pro
│   └── google-services.json                # Firebase configuration
├── gradle/
│   ├── libs.versions.toml                  # Version catalog
│   └── wrapper/
├── build.gradle.kts                        # Project-level build config
├── settings.gradle.kts
├── gradle.properties
└── .gitignore
```

### Directory Organization Principles

1. **Feature-based UI structure:** Group UI components by feature (`/ui/auth`, `/ui/home`, `/ui/trips`)
2. **Separation of concerns:** Clear boundaries between UI, ViewModel, and Data layers
3. **Single Activity architecture:** One `MainActivity` with Compose-based navigation
4. **Centralized theme:** All design tokens in `/ui/theme`

---

## Architecture & Patterns

### MVVM (Model-View-ViewModel) Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    View Layer (UI)                      │
│  Composable functions in /ui/* (LoginScreen, etc.)     │
└────────────────┬────────────────────────────────────────┘
                 │ observes StateFlow
                 ▼
┌─────────────────────────────────────────────────────────┐
│                  ViewModel Layer                        │
│     AuthViewModel.kt - manages auth state               │
│     Uses StateFlow for reactive state updates           │
└────────────────┬────────────────────────────────────────┘
                 │ calls repository methods
                 ▼
┌─────────────────────────────────────────────────────────┐
│                   Data Layer                            │
│  TripRepository.kt - interfaces with Firebase           │
│  Trip.kt - data models                                  │
└─────────────────────────────────────────────────────────┘
```

### Key Patterns

#### 1. State Management with Sealed Classes
```kotlin
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
```
**When to use:** For representing distinct states in ViewModels (loading, success, error, etc.)

#### 2. Repository Pattern
```kotlin
class TripRepository {
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val tripsCollection = firestore.collection("trips")

    suspend fun createTrip(...): Result<String> { /* ... */ }
    fun getUserTrips(): Flow<List<Trip>> = callbackFlow { /* ... */ }
}
```
**When to use:** All data access operations. Repository is the single source of truth for data.

#### 3. Flow-based Real-time Updates
```kotlin
fun getUserTrips(): Flow<List<Trip>> = callbackFlow {
    val subscription = tripsCollection
        .whereEqualTo("createdBy", userId)
        .addSnapshotListener { snapshot, error ->
            // Emit updates
            trySend(trips)
        }
    awaitClose { subscription.remove() }
}
```
**When to use:** Real-time data synchronization with Firebase Firestore.

#### 4. Composable Preview Functions
```kotlin
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            userEmail = "test@example.com",
            onCreateTripClick = {},
            onLogoutClick = {}
        )
    }
}
```
**Always include:** Preview functions for all screen-level composables.

---

## Coding Conventions

### Kotlin Style

1. **Naming Conventions**
   - Files: PascalCase (e.g., `AuthViewModel.kt`, `HomeScreen.kt`)
   - Classes/Objects: PascalCase (e.g., `AuthViewModel`, `TripRepository`)
   - Functions: camelCase (e.g., `getUserTrips()`, `createTrip()`)
   - Variables: camelCase (e.g., `userEmail`, `tripList`)
   - Constants: UPPER_SNAKE_CASE (if needed)

2. **File Organization**
   - Package declaration first
   - Imports (Android, then third-party, then project imports)
   - Class/object declaration
   - Properties before methods
   - Public methods before private methods

3. **Compose Conventions**
   - Composable function names: PascalCase (e.g., `LoginScreen`, `TripCard`)
   - Modifier parameter first in composables
   - Default parameter values for optional callbacks
   - Hoist state to parent composables

### Example Screen Structure
```kotlin
package com.lorenda.groupgo.ui.feature

import androidx.compose.runtime.*
// ... other imports

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(
    modifier: Modifier = Modifier,          // Modifier first
    data: DataType,                         // Required params
    onActionClick: () -> Unit = {},         // Callbacks with defaults
) {
    // Local state
    var localState by remember { mutableStateOf(false) }

    // Effects
    LaunchedEffect(key1) { /* side effects */ }

    // UI Structure
    Scaffold(/* ... */) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Content
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeatureScreenPreview() {
    MaterialTheme {
        FeatureScreen(data = mockData)
    }
}
```

### Data Models
```kotlin
data class ModelName(
    val id: String = "",                    // Defaults for Firebase
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```
**Always provide default values** for Firestore serialization.

### ViewModel Pattern
```kotlin
class FeatureViewModel : ViewModel() {
    // Private mutable state
    private val _state = MutableStateFlow<State>(State.Idle)
    // Public immutable state
    val state: StateFlow<State> = _state

    init {
        // Initialize state
    }

    fun performAction() {
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                // Async operation
                _state.value = State.Success(result)
            } catch (e: Exception) {
                _state.value = State.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

---

## Development Workflows

### Adding a New Feature

1. **Create Feature Package**
   ```
   app/src/main/java/com/lorenda/groupgo/ui/[feature]/
   ```

2. **Define Data Model (if needed)**
   ```kotlin
   // data/FeatureModel.kt
   data class FeatureModel(
       val id: String = "",
       val field: String = "",
       val createdAt: Long = System.currentTimeMillis()
   )
   ```

3. **Create Repository (if needed)**
   ```kotlin
   // data/FeatureRepository.kt
   class FeatureRepository {
       private val firestore = Firebase.firestore
       private val collection = firestore.collection("collection_name")

       suspend fun createItem(...): Result<String> { /* ... */ }
       fun getItems(): Flow<List<Item>> = callbackFlow { /* ... */ }
   }
   ```

4. **Create ViewModel (if complex state)**
   ```kotlin
   // FeatureViewModel.kt
   class FeatureViewModel : ViewModel() {
       private val repository = FeatureRepository()
       private val _state = MutableStateFlow<FeatureState>(FeatureState.Idle)
       val state: StateFlow<FeatureState> = _state
   }
   ```

5. **Create Screen Composable**
   ```kotlin
   // ui/feature/FeatureScreen.kt
   @Composable
   fun FeatureScreen(/* ... */) { /* ... */ }
   ```

6. **Integrate into Navigation**
   - Add navigation logic in `MainActivity.kt` or create navigation graph
   - Use `remember { mutableStateOf() }` for screen state management

7. **Add Preview Function**
   - Always include `@Preview` for visual testing

### Making Changes to Existing Features

1. **Read existing code first** to understand current patterns
2. **Maintain consistency** with existing code style
3. **Update related components:**
   - If changing data model → update Repository, ViewModel, and UI
   - If changing UI → ensure ViewModel contract is maintained
4. **Test changes** with preview functions and/or unit tests

### Working with Firebase

1. **Authentication**
   ```kotlin
   val auth: FirebaseAuth = Firebase.auth

   // Sign in
   auth.signInWithEmailAndPassword(email, password).await()

   // Get current user
   val userId = auth.currentUser?.uid
   ```

2. **Firestore Operations**
   ```kotlin
   // Create
   collection.document(id).set(data).await()

   // Read with real-time updates
   collection.whereEqualTo("field", value)
       .addSnapshotListener { snapshot, error ->
           // Handle updates
       }

   // Delete
   collection.document(id).delete().await()
   ```

3. **Always wrap in try-catch and return Result**
   ```kotlin
   suspend fun operation(): Result<ReturnType> {
       return try {
           // Firebase operation
           Result.success(value)
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```

---

## Firebase Integration

### Configuration
- **File:** `app/google-services.json` (git-ignored, contains Firebase project config)
- **Plugin:** `com.google.gms.google-services` version 4.4.2

### Collections Structure

#### `trips` Collection
```json
{
  "id": "document_id",
  "name": "Trip Name",
  "destination": "Location",
  "startDate": "TBD",
  "endDate": "TBD",
  "budget": "1000",
  "numberOfPeople": "5",
  "createdBy": "firebase_user_uid",
  "createdAt": 1700000000000
}
```

### Security Considerations
1. **Always check user authentication** before Firestore operations
2. **User-specific queries:** Use `auth.currentUser?.uid` to filter data
3. **Error handling:** Always wrap Firebase calls in try-catch
4. **Offline persistence:** Firestore has built-in offline support

---

## UI/UX Guidelines

### Material Design 3
- Use `MaterialTheme.colorScheme.*` for colors
- Use `MaterialTheme.typography.*` for text styles
- Dynamic color support (Android 12+) via `GroupGoTheme`

### Color Usage
```kotlin
// Primary actions, app bar
MaterialTheme.colorScheme.primary
MaterialTheme.colorScheme.onPrimary

// Cards, highlighted content
MaterialTheme.colorScheme.primaryContainer
MaterialTheme.colorScheme.onPrimaryContainer

// Errors
MaterialTheme.colorScheme.error
```

### Common Components

1. **Screen Structure**
   ```kotlin
   Scaffold(
       topBar = { TopAppBar(/* ... */) },
       floatingActionButton = { FAB(/* ... */) }
   ) { paddingValues ->
       Content(modifier = Modifier.padding(paddingValues))
   }
   ```

2. **Loading States**
   ```kotlin
   if (isLoading) {
       CircularProgressIndicator()
   }
   ```

3. **Empty States**
   - Use centered Column with icon, text, and action button
   - See `HomeScreen.kt:105-143` for reference

4. **Dialogs**
   ```kotlin
   AlertDialog(
       onDismissRequest = { /* dismiss */ },
       title = { Text("Title") },
       text = { Text("Message") },
       confirmButton = { TextButton(/* ... */) },
       dismissButton = { TextButton(/* ... */) }
   )
   ```

### User Feedback
- Use `Toast` for transient messages (success, errors)
- Use `AlertDialog` for confirmation actions (delete, etc.)
- Use `Snackbar` for actions with undo (future enhancement)

---

## Testing Strategy

### Current State
- Basic unit test structure in `app/src/test/`
- Instrumented test setup in `app/src/androidTest/`
- **Coverage:** Minimal (needs expansion)

### Testing Recommendations

#### Unit Tests (JUnit)
```kotlin
// app/src/test/java/com/lorenda/groupgo/
class RepositoryTest {
    @Test
    fun `createTrip returns success on valid input`() {
        // Arrange, Act, Assert
    }
}
```

#### Composable Tests
```kotlin
// app/src/androidTest/java/com/lorenda/groupgo/
class ScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreenDisplaysCorrectly() {
        composeTestRule.setContent {
            LoginScreen(/* ... */)
        }

        composeTestRule
            .onNodeWithText("Login")
            .assertIsDisplayed()
    }
}
```

### When Adding Tests
1. **Unit test ViewModels:** State transitions, error handling
2. **Unit test Repositories:** Mock Firebase, test data transformations
3. **UI tests for critical flows:** Login, trip creation
4. **Test edge cases:** Empty states, error states, loading states

---

## Common Tasks & Examples

### Adding a New Screen

```kotlin
// 1. Create file: ui/feature/NewScreen.kt
package com.lorenda.groupgo.ui.feature

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Feature") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text("New Screen Content")
        }
    }
}

@Preview
@Composable
fun NewScreenPreview() {
    MaterialTheme {
        NewScreen()
    }
}

// 2. Add navigation in MainActivity.kt
var showNewScreen by remember { mutableStateOf(false) }

when {
    showNewScreen -> {
        NewScreen(onBackClick = { showNewScreen = false })
    }
    // ... other screens
}
```

### Adding a New Data Model with Firebase

```kotlin
// 1. Define model: data/NewModel.kt
data class NewModel(
    val id: String = "",
    val field1: String = "",
    val field2: Int = 0,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// 2. Create repository: data/NewModelRepository.kt
class NewModelRepository {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val collection = firestore.collection("new_collection")

    suspend fun createItem(field1: String, field2: Int): Result<String> {
        return try {
            val userId = auth.currentUser?.uid
                ?: throw Exception("User not logged in")

            val item = NewModel(
                id = collection.document().id,
                field1 = field1,
                field2 = field2,
                createdBy = userId
            )

            collection.document(item.id).set(item).await()
            Result.success(item.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserItems(): Flow<List<NewModel>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = collection
            .whereEqualTo("createdBy", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(NewModel::class.java)
                } ?: emptyList()

                trySend(items)
            }

        awaitClose { subscription.remove() }
    }
}
```

### Adding a ViewModel

```kotlin
class NewFeatureViewModel : ViewModel() {
    private val repository = NewFeatureRepository()

    private val _state = MutableStateFlow<FeatureState>(FeatureState.Idle)
    val state: StateFlow<FeatureState> = _state

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    init {
        loadItems()
    }

    private fun loadItems() {
        viewModelScope.launch {
            repository.getItems().collect { itemList ->
                _items.value = itemList
            }
        }
    }

    fun performAction(param: String) {
        viewModelScope.launch {
            _state.value = FeatureState.Loading
            try {
                val result = repository.doSomething(param)
                if (result.isSuccess) {
                    _state.value = FeatureState.Success("Action completed")
                } else {
                    _state.value = FeatureState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error"
                    )
                }
            } catch (e: Exception) {
                _state.value = FeatureState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetState() {
        _state.value = FeatureState.Idle
    }
}

sealed class FeatureState {
    object Idle : FeatureState()
    object Loading : FeatureState()
    data class Success(val message: String) : FeatureState()
    data class Error(val message: String) : FeatureState()
}
```

---

## Troubleshooting

### Common Issues

#### Firebase Connection Issues
- **Problem:** "User not logged in" errors
- **Solution:** Check `FirebaseAuth.getInstance().currentUser != null` before operations
- **Location:** Repository functions should always verify auth state

#### Build Issues
- **Problem:** "Unresolved reference" for Firebase
- **Solution:** Ensure `google-services.json` is present in `app/` directory
- **Check:** Sync Gradle files after adding dependencies

#### Compose Preview Not Showing
- **Problem:** Preview doesn't render
- **Solution:**
  1. Check `@Preview` annotation is present
  2. Ensure preview function has no parameters or provides defaults
  3. Wrap content in `MaterialTheme` or `GroupGoTheme`
  4. Rebuild project

#### State Not Updating in UI
- **Problem:** UI doesn't reflect StateFlow changes
- **Solution:**
  1. Use `collectAsState()` to observe StateFlow in composables
  2. Ensure StateFlow emission is happening on main thread
  3. Check if state is actually changing (add logs)

#### Navigation Issues
- **Problem:** Screen doesn't switch when state changes
- **Solution:** Verify the `when` conditions in `GroupGoApp` are correct and mutually exclusive

### Debug Checklist

When debugging an issue:
1. Check Logcat for errors/exceptions
2. Verify Firebase Authentication state
3. Check network connectivity for Firebase operations
4. Ensure StateFlow emissions are happening
5. Verify composable recomposition with layout inspector
6. Check for null values from Firebase queries

---

## Build & Deployment

### Build Configuration

**Debug Build:**
```bash
./gradlew assembleDebug
```

**Release Build:**
```bash
./gradlew assembleRelease
```
Note: Currently ProGuard/R8 is disabled (`isMinifyEnabled = false`)

### Version Management
Update version in `app/build.gradle.kts`:
```kotlin
versionCode = 1        // Increment for each release
versionName = "1.0"    // Semantic versioning
```

### Firebase Setup for New Environments
1. Create Firebase project at https://console.firebase.google.com
2. Add Android app with package name `com.lorenda.groupgo`
3. Download `google-services.json`
4. Place in `app/` directory
5. Enable Authentication (Email/Password)
6. Create Firestore database
7. Set up Firestore security rules (see below)

### Firestore Security Rules
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /trips/{tripId} {
      allow read, write: if request.auth != null
                         && request.auth.uid == resource.data.createdBy;
      allow create: if request.auth != null
                    && request.auth.uid == request.resource.data.createdBy;
    }
  }
}
```

---

## Git Workflow

### Branch Naming
- Feature branches: `feature/description`
- Bug fixes: `bugfix/description`
- Current development: `claude/claude-md-mi6vons0tttjskss-016B4rKCJYxD6TwSRMqKY9YB`

### Commit Message Style (from git history)
- Present tense, descriptive
- Examples:
  - "Added Trip Delete Functionality"
  - "Added Ability to create and store trips to Firebase"
  - "Complete authentication system with login, signup, and logout"

### Files to Never Commit
- `google-services.json` (contains API keys)
- `local.properties`
- `.idea/` directory contents
- Build outputs: `/build`, `/app/build`

---

## AI Assistant Guidelines

### When Working on This Codebase

1. **Always Follow MVVM:** UI → ViewModel → Repository → Firebase
2. **Maintain Existing Patterns:** Review similar existing code before implementing
3. **Use Kotlin Idioms:** Prefer Kotlin standard library functions, use scope functions appropriately
4. **State Management:** Always use StateFlow for reactive state in ViewModels
5. **Error Handling:** Wrap Firebase operations in try-catch, return Result types
6. **User Feedback:** Show Toast for success/error messages
7. **Compose Best Practices:**
   - Hoist state to parent composables
   - Use `remember` for local UI state
   - Use `LaunchedEffect` for side effects
   - Provide default parameter values for callbacks
8. **Testing:** Add preview functions for all new screens
9. **Documentation:** Add KDoc comments for complex logic
10. **Security:** Never commit sensitive data, always verify user authentication

### Before Making Changes

1. Read relevant existing code
2. Understand the current pattern
3. Check if similar functionality exists
4. Verify Firebase integration points
5. Consider impact on existing features

### After Making Changes

1. Ensure code compiles
2. Test with preview functions if UI changes
3. Verify Firebase operations work (if applicable)
4. Check for any introduced security issues
5. Update this CLAUDE.md if architecture changes

---

## Resources

### Official Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Firebase Android](https://firebase.google.com/docs/android/setup)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Material Design 3](https://m3.material.io/)

### Project-Specific Files to Reference
- `MainActivity.kt` - Main navigation and app structure
- `AuthViewModel.kt` - State management example
- `TripRepository.kt` - Firebase integration pattern
- `HomeScreen.kt` - Complex UI with empty states, dialogs
- `Theme.kt` - Material 3 theming setup

---

## Version History

| Version | Date       | Changes                                    |
|---------|------------|--------------------------------------------|
| 1.0     | 2025-11-20 | Initial CLAUDE.md creation                 |

---

**Questions or Improvements?**
If you discover better patterns or have suggestions for improving this guide, update this document and increment the version number.
