package com.posbah.app.auth

import android.app.Activity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.auth0.android.jwt.JWT
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.posbah.app.BuildConfig
import java.security.SecureRandom
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInClient @Inject constructor(
    private val credentialManager: CredentialManager
) {

    sealed class Result {
        data class Success(val identity: GoogleIdentity) : Result()
        object Cancelled : Result()
        sealed class Error : Result() {
            data class NoCredentials(val message: String?) : Error()
            data class InvalidToken(val reason: String) : Error()
            data class Unexpected(val throwable: Throwable) : Error()
        }
    }

    data class GoogleIdentity(
        val sub: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?,
        val rawIdToken: String,
        val issuedAt: Long,
        val expiresAt: Long
    )

    private fun randomNonce(): String {
        val b = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return android.util.Base64.encodeToString(b, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
    }

    suspend fun signIn(activity: Activity): Result {
        val nonce = randomNonce()
        // First pass: prefer previously-authorized accounts (returning user).
        val returningOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()
        val returningRequest = GetCredentialRequest.Builder()
            .addCredentialOption(returningOption)
            .build()

        return try {
            val resp = credentialManager.getCredential(activity, returningRequest)
            handleResponse(resp.credential, nonce)
        } catch (e: NoCredentialException) {
            // Fall back: allow new account / sign-up
            val newNonce = randomNonce()
            val signUpOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setNonce(newNonce)
                .build()
            val signUpRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signUpOption)
                .build()
            try {
                val resp = credentialManager.getCredential(activity, signUpRequest)
                handleResponse(resp.credential, newNonce)
            } catch (e2: NoCredentialException) {
                Result.Error.NoCredentials(e2.message)
            } catch (e2: GetCredentialException) {
                Result.Error.Unexpected(e2)
            }
        } catch (e: GetCredentialException) {
            // Cancellation manifests as GetCredentialCancellationException
            if (e.javaClass.simpleName.contains("Cancellation", ignoreCase = true)) {
                Result.Cancelled
            } else Result.Error.Unexpected(e)
        } catch (e: Exception) {
            Result.Error.Unexpected(e)
        }
    }

    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun handleResponse(credential: androidx.credentials.Credential, expectedNonce: String): Result {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return Result.Error.InvalidToken("Unsupported credential type: ${credential::class.simpleName}")
        }
        val tokenCred = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = tokenCred.idToken
        val validation = validateIdToken(idToken, expectedNonce)
        return when (validation) {
            is Validation.Valid -> Result.Success(
                GoogleIdentity(
                    sub = validation.sub,
                    email = validation.email ?: tokenCred.id,
                    displayName = validation.name ?: tokenCred.displayName,
                    photoUrl = tokenCred.profilePictureUri?.toString(),
                    rawIdToken = idToken,
                    issuedAt = validation.iat,
                    expiresAt = validation.exp
                )
            )
            is Validation.Invalid -> Result.Error.InvalidToken(validation.reason)
        }
    }

    private sealed class Validation {
        data class Valid(
            val sub: String,
            val email: String?,
            val name: String?,
            val iat: Long,
            val exp: Long
        ) : Validation()
        data class Invalid(val reason: String) : Validation()
    }

    private fun validateIdToken(idToken: String, expectedNonce: String): Validation {
        return try {
            val jwt = JWT(idToken)
            val iss = jwt.issuer ?: return Validation.Invalid("missing iss")
            if (iss != "accounts.google.com" && iss != "https://accounts.google.com") {
                return Validation.Invalid("bad iss: $iss")
            }
            val audClaim = jwt.audience
            if (audClaim == null || !audClaim.contains(BuildConfig.GOOGLE_WEB_CLIENT_ID)) {
                return Validation.Invalid("bad aud: $audClaim")
            }
            val exp = jwt.expiresAt
            if (exp == null || exp.before(Date())) {
                return Validation.Invalid("token expired")
            }
            val nonce = jwt.getClaim("nonce").asString()
            if (!nonce.isNullOrEmpty() && nonce != expectedNonce) {
                return Validation.Invalid("nonce mismatch")
            }
            val sub = jwt.subject ?: return Validation.Invalid("missing sub")
            Validation.Valid(
                sub = sub,
                email = jwt.getClaim("email").asString(),
                name = jwt.getClaim("name").asString(),
                iat = jwt.issuedAt?.time ?: System.currentTimeMillis(),
                exp = exp.time
            )
        } catch (e: Exception) {
            Validation.Invalid("decode failed: ${e.message}")
        }
    }
}
