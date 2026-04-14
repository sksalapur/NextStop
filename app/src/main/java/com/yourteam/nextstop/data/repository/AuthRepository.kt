package com.yourteam.nextstop.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.yourteam.nextstop.BuildConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Launch the Google Sign In flow using Android Credential Manager.
     */
    suspend fun signInWithGoogle(context: Context): FirebaseUser {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            
            // Generate Firebase Credential from Google ID Token
            val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = auth.signInWithCredential(authCredential).await()
            return authResult.user ?: throw Exception("Google sign in failed")
        } else {
            throw Exception("Unknown credential type or Google Sign-In failed.")
        }
    }

    /**
     * Initializes the user role upon their first login.
     * Looks up the user's email in the `invited_drivers` collection to determine if they are a driver.
     * @return the assigned role.
     */
    suspend fun handleUserRoleInitialization(user: FirebaseUser): String {
        val uid = user.uid
        val email = user.email?.lowercase() ?: throw Exception("Email is required for sign in")
        val name = user.displayName ?: "User"

        val userDocRef = firestore.collection("users").document(uid)
        val snapshot = userDocRef.get().await()

        if (snapshot.exists()) {
            return snapshot.getString("role") ?: throw Exception("Role field missing in user profile")
        } else {
            // New User Registration Flow
            val invitedDoc = firestore.collection("invited_drivers").document(email).get().await()
            val role = if (invitedDoc.exists()) {
                // Consume the invitation
                firestore.collection("invited_drivers").document(email).delete().await()
                "driver"
            } else {
                "student" // Default fallback
            }

            // Create new users document
            val newUser = hashMapOf(
                "name" to name,
                "email" to email,
                "role" to role
            )
            userDocRef.set(newUser).await()
            return role
        }
    }

    /**
     * Sign out the current user.
     */
    fun logout() {
        auth.signOut()
    }
}
