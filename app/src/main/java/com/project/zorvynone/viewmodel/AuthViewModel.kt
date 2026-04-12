package com.project.zorvynone.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.project.zorvynone.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // ── Auth UI State ────────────────────────────────────────────────────────
    sealed class AuthState {
        data object Idle : AuthState()
        data object Loading : AuthState()
        data class Success(val user: FirebaseUser) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _resetEmailSent = MutableStateFlow(false)
    val resetEmailSent = _resetEmailSent.asStateFlow()

    val currentUser: FirebaseUser? get() = auth.currentUser

    // ── Email/Password Sign Up ───────────────────────────────────────────────
    fun signUpWithEmail(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                // Set display name
                auth.currentUser?.updateProfile(
                    userProfileChangeRequest { displayName = username }
                )?.await()
                Log.d("AuthVM", "Sign up success: ${auth.currentUser?.email}")
                _authState.value = AuthState.Success(auth.currentUser!!)
            } catch (e: Exception) {
                Log.e("AuthVM", "Sign up failed", e)
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    // ── Email/Password Sign In ───────────────────────────────────────────────
    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                Log.d("AuthVM", "Sign in success: ${auth.currentUser?.email}")
                _authState.value = AuthState.Success(auth.currentUser!!)
            } catch (e: Exception) {
                Log.e("AuthVM", "Sign in failed", e)
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    // ── Google Sign-In (Credential Manager) ──────────────────────────────────
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)

                val webClientId = BuildConfig.WEB_CLIENT_ID
                if (webClientId.isBlank()) {
                    throw Exception("Google Sign-In not configured. Add WEB_CLIENT_ID to local.properties.")
                }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                auth.signInWithCredential(firebaseCredential).await()
                Log.d("AuthVM", "Google sign in success: ${auth.currentUser?.email}")
                _authState.value = AuthState.Success(auth.currentUser!!)
            } catch (e: Exception) {
                Log.e("AuthVM", "Google sign in failed", e)
                _authState.value = AuthState.Error("Google sign-in failed. Please try again.")
            }
        }
    }

    // ── Forgot Password ──────────────────────────────────────────────────────
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                _resetEmailSent.value = true
                Log.d("AuthVM", "Password reset email sent to $email")
            } catch (e: Exception) {
                Log.e("AuthVM", "Password reset failed", e)
                _authState.value = AuthState.Error(mapFirebaseError(e))
            }
        }
    }

    // ── Sign Out ─────────────────────────────────────────────────────────────
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Idle
        Log.d("AuthVM", "User signed out")
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun clearResetFlag() {
        _resetEmailSent.value = false
    }

    /**
     * Maps common Firebase Auth exceptions to user-friendly messages.
     */
    private fun mapFirebaseError(e: Exception): String {
        val msg = e.message ?: return "An unknown error occurred."
        return when {
            msg.contains("INVALID_LOGIN_CREDENTIALS", true) ||
            msg.contains("INVALID_EMAIL", true) ->
                "Invalid email or password. Please try again."
            msg.contains("EMAIL_EXISTS", true) ||
            msg.contains("email address is already in use", true) ->
                "An account with this email already exists."
            msg.contains("WEAK_PASSWORD", true) ||
            msg.contains("at least 6 characters", true) ->
                "Password must be at least 6 characters."
            msg.contains("USER_NOT_FOUND", true) ||
            msg.contains("no user record", true) ->
                "No account found with this email."
            msg.contains("network", true) ->
                "Network error. Check your connection."
            msg.contains("TOO_MANY_REQUESTS", true) ->
                "Too many attempts. Please wait a moment."
            else -> msg.take(100)
        }
    }
}
