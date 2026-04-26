package com.project.zorvynone

import android.content.Context
import android.util.Log
import io.kommunicate.Kommunicate
import io.kommunicate.callbacks.KMLoginHandler
import io.kommunicate.callbacks.KmCallback
import io.kommunicate.users.KMUser
import io.kommunicate.devkit.api.account.register.RegistrationResponse
import io.kommunicate.KmConversationBuilder
import java.util.UUID

/**
 * Helper object encapsulating all Kommunicate SDK operations.
 * Handles user login, initialization, and conversation launch.
 */
object KommunicateHelper {

    private const val TAG = "KommunicateHelper"

    // Replace with your actual Kommunicate APP_ID from the dashboard
    const val APP_ID = "1eee7105d984bad7a8465d50dc58e0ab3"

    /**
     * Logs in a user to Kommunicate with a unique generated userId.
     * Must be called before opening any conversation.
     *
     * @param context Application or Activity context
     * @param displayName Optional display name for the chat user
     * @param onSuccess Called when login succeeds
     * @param onFailure Called with error message when login fails
     */
    fun loginUser(
        context: Context,
        displayName: String = "Expectr User",
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        try {
            // Skip if already logged in
            if (KMUser.isLoggedIn(context)) {
                Log.d(TAG, "Kommunicate user already logged in")
                onSuccess()
                return
            }

            val kmUser = KMUser().apply {
                userId = "expectr_${UUID.randomUUID()}"
                this.displayName = displayName
            }

            Kommunicate.init(context, APP_ID)

            Kommunicate.login(context, kmUser, object : KMLoginHandler {
                override fun onSuccess(
                    registrationResponse: RegistrationResponse?,
                    context: Context?
                ) {
                    Log.d(TAG, "Kommunicate login successful for user: ${kmUser.userId}")
                    onSuccess()
                }

                override fun onFailure(
                    registrationResponse: RegistrationResponse?,
                    exception: Exception?
                ) {
                    val errorMsg = exception?.message ?: "Unknown login error"
                    Log.e(TAG, "Kommunicate login failed: $errorMsg", exception)
                    onFailure(errorMsg)
                }
            })
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unexpected error during Kommunicate login"
            Log.e(TAG, errorMsg, e)
            onFailure(errorMsg)
        }
    }

    /**
     * Opens the Kommunicate conversation screen.
     * Ensure [loginUser] has been called and succeeded before invoking this.
     *
     * @param context Activity context
     */
    fun openChat(context: Context) {
        try {
            KmConversationBuilder(context)
                .launchConversation(object : KmCallback {
                    override fun onSuccess(message: Any) {
                        Log.d(TAG, "Conversation launched successfully: $message")
                    }

                    override fun onFailure(error: Any) {
                        Log.e(TAG, "Failed to launch conversation: $error")
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Kommunicate conversation: ${e.message}", e)
        }
    }
}
