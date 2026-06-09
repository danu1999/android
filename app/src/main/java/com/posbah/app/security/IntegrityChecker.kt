package com.posbah.app.security

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Wraps the Play Integrity API which gives a cryptographically signed verdict
 * that the running app binary is genuine and unmodified, the device is recognized,
 * and the install came from Play. The token is opaque to us (real verification
 * happens server-side using Google's public keys). Since this is a backend-less
 * app, we only fetch the token to make tampering detectable & log-able, and we
 * complement it with strict on-device signature pinning.
 *
 * For production: nonce should be issued by your server. Here we generate locally
 * for opportunistic integrity logging.
 */
class IntegrityChecker(private val context: Context) {

    /**
     * Compute SHA-256 of the APK signing certificate. Compare against the
     * compile-time pinned digest to detect repackaged / re-signed APKs.
     */
    fun appSignatureSha256(): String? {
        return runCatching {
            val pm = context.packageManager
            val pkgName = context.packageName
            val signatures: Array<Signature> = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES).signatures ?: emptyArray()
            }
            if (signatures.isEmpty()) return null
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatures[0].toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        }.getOrNull()
    }

    /**
     * Get a Play Integrity verdict token. Without a backend we cannot fully verify
     * it cryptographically, but presence + ability to fetch is itself a signal:
     * - Re-packaged APKs will see Play Integrity API refuse to issue a token
     * - Unverified devices will get verdicts with lower assurance fields
     * Returns null on failure (treated as "not verifiable" by caller).
     */
    suspend fun fetchIntegrityToken(): String? = runCatching {
        val nonce = java.util.UUID.randomUUID().toString().replace("-", "")
        val manager = IntegrityManagerFactory.create(context)
        val req = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .build()
        val response = manager.requestIntegrityToken(req).await()
        response.token()
    }.getOrNull()
}
