package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ChatRepository
import com.example.data.firebase.FirestoreSyncRepository
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutFromFirebase
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.theme.AppSkinTheme
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppContextProvider.init(this)
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        FirebaseApp.initializeApp(this)
      }
    } catch (_: Exception) {
      // Safe fallback in unit test environments without google-services processing
    }
    enableEdgeToEdge()
    setContent {
      AppContent()
    }
  }
}

@Composable
fun AppContent() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val firebaseAuth: FirebaseAuth? = remember {
    try {
      Firebase.auth
    } catch (_: Exception) {
      null
    }
  }

  var currentUser by remember { mutableStateOf<FirebaseUser?>(firebaseAuth?.currentUser) }

  DisposableEffect(firebaseAuth) {
    if (firebaseAuth == null) {
      onDispose {}
    } else {
      val listener = FirebaseAuth.AuthStateListener { auth ->
        currentUser = auth.currentUser
      }
      firebaseAuth.addAuthStateListener(listener)
      onDispose {
        firebaseAuth.removeAuthStateListener(listener)
      }
    }
  }

  LaunchedEffect(firebaseAuth) {
    if (firebaseAuth != null && firebaseAuth.currentUser == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = { currentUser = firebaseAuth.currentUser },
        onUnauthenticated = {},
        scope = scope
      )
    }
  }

  val signedInUser = currentUser
  val uid = signedInUser?.uid ?: ""
  val sessionKey = uid.ifBlank { "guest" }

  key(sessionKey) {
    val viewModel: ChatViewModel = viewModel(
      key = sessionKey,
      factory = viewModelFactory {
        initializer {
          val app = checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
          val syncRepo = if (signedInUser != null) {
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            FirestoreSyncRepository(db)
          } else {
            null
          }
          val email = signedInUser?.email ?: ""
          val displayName = if (signedInUser != null) {
            signedInUser.displayName ?: email.substringBefore("@").ifBlank { "User" }
          } else {
            ""
          }
          ChatViewModel(
            repository = ChatRepository(),
            firestoreSync = syncRepo,
            authenticatedUid = uid,
            authenticatedEmail = email,
            authenticatedDisplayName = displayName,
            authenticatedPhotoUrl = signedInUser?.photoUrl?.toString()
          )
        }
      }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentSkin = AppSkinTheme.fromId(uiState.appSkinThemeId)

    MyApplicationTheme(skinTheme = currentSkin) {
      Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
      ) {
        ChatScreen(
          viewModel = viewModel,
          onSignOut = {
            signOutFromFirebase(
              credentialManager = credentialManager,
              scope = scope,
              onBeforeSignOut = { viewModel.prepareSignOut() },
              onSignOutComplete = { currentUser = null }
            )
          }
        )
      }
    }
  }
}
