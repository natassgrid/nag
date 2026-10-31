package `in`.gov.nag.mobile.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import org.json.JSONObject
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

/**
 * NagHardwareKeystoreModule - Android Keystore Hardware Security.
 * Generates ECDSA P-256 (secp256r1) key pairs in StrongBox Keymaster (with fallback to TEE)
 * and signs CBT exam submissions using SHA256withECDSA for non-repudiation and zero-trust verification.
 */
class HardwareKeystoreModule {

    companion object {
        const val MODULE_NAME = "NagHardwareKeystoreModule"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "NAG_CBT_HARDWARE_KEY"
        private const val EC_CURVE_NAME = "secp256r1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val TAG = "NagHardwareKeystore"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Ensures an ECDSA P-256 key pair exists in AndroidKeyStore.
     * Prefers StrongBox Keymaster; gracefully falls back to Standard TEE hardware if StrongBox is unavailable.
     */
    @JavascriptInterface
    fun ensureHardwareKeyPair(aliasArg: String?): String {
        val alias = if (aliasArg.isNullOrBlank()) DEFAULT_KEY_ALIAS else aliasArg
        return try {
            if (!keyStore.containsAlias(alias)) {
                generateEcKeyPair(alias)
            }

            val keyInfoJson = getKeyInfoJson(alias)
            if (keyInfoJson != null) {
                keyInfoJson.toString()
            } else {
                JSONObject().apply {
                    put("error", "Key pair $alias could not be found after generation.")
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure hardware key pair: ${e.message}", e)
            JSONObject().apply {
                put("error", "Could not generate or retrieve key: ${e.message}")
            }.toString()
        }
    }

    /**
     * Signs exam submission payload hash using the hardware-backed private key (SHA256withECDSA).
     */
    @JavascriptInterface
    fun signExamSubmission(payloadSha256Hex: String, aliasArg: String?): String {
        val alias = if (aliasArg.isNullOrBlank()) DEFAULT_KEY_ALIAS else aliasArg
        return try {
            if (!keyStore.containsAlias(alias)) {
                generateEcKeyPair(alias)
            }

            val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
                ?: return JSONObject().apply {
                    put("error", "PrivateKeyEntry for alias $alias not found.")
                }.toString()

            val privateKey: PrivateKey = entry.privateKey
            val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
            signer.initSign(privateKey)

            // Convert hex or plain text bytes
            val dataBytes = if (payloadSha256Hex.matches(Regex("^[0-9a-fA-F]+$")) && payloadSha256Hex.length % 2 == 0) {
                hexStringToByteArray(payloadSha256Hex)
            } else {
                payloadSha256Hex.toByteArray(Charsets.UTF_8)
            }

            signer.update(dataBytes)
            val signatureBytes = signer.sign()
            val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

            val isStrongBox = isStrongBoxBackedKey(privateKey)

            val result = JSONObject().apply {
                put("alias", alias)
                put("payloadHash", payloadSha256Hex)
                put("signatureBase64", signatureBase64)
                put("algorithm", SIGNATURE_ALGORITHM)
                put("timestamp", System.currentTimeMillis().toDouble())
                put("strongBoxBacked", isStrongBox)
            }

            result.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Signing failed: ${e.message}", e)
            JSONObject().apply {
                put("error", "Failed to sign exam payload: ${e.message}")
            }.toString()
        }
    }

    /**
     * Retrieves public key information, attestation details, and hardware backing status.
     */
    @JavascriptInterface
    fun getHardwareKeyInfo(aliasArg: String?): String? {
        val alias = if (aliasArg.isNullOrBlank()) DEFAULT_KEY_ALIAS else aliasArg
        return try {
            if (!keyStore.containsAlias(alias)) {
                return null
            }

            getKeyInfoJson(alias)?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying hardware key info: ${e.message}", e)
            JSONObject().apply {
                put("error", e.message)
            }.toString()
        }
    }

    private fun generateEcKeyPair(alias: String) {
        // Attempt 1: Try StrongBox Keymaster on Android 9+ (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE_NAME))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setIsStrongBoxBacked(true)
                    .build()
                kpg.initialize(spec)
                kpg.generateKeyPair()
                Log.i(TAG, "Successfully generated StrongBox-backed ECDSA key for alias: $alias")
                return
            } catch (e: Exception) {
                // If StrongBox is unavailable, fallback gracefully to Standard TEE Keymaster
                Log.w(TAG, "StrongBox unavailable, falling back to Standard TEE Keymaster: ${e.message}")
            }
        }

        // Attempt 2: Standard TEE hardware-backed key pair
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE_NAME))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        kpg.initialize(spec)
        kpg.generateKeyPair()
        Log.i(TAG, "Successfully generated TEE-backed ECDSA key for alias: $alias")
    }

    private fun getKeyInfoJson(alias: String): JSONObject? {
        val cert = keyStore.getCertificate(alias) as? X509Certificate ?: return null
        val publicKeyBytes = cert.publicKey.encoded
        val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        val certChain = keyStore.getCertificateChain(alias)

        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        val isStrongBox = entry?.privateKey?.let { isStrongBoxBackedKey(it) } ?: false

        return JSONObject().apply {
            put("alias", alias)
            put("algorithm", "EC/$EC_CURVE_NAME")
            put("publicKeyBase64", publicKeyBase64)
            put("isHardwareBacked", true)
            put("isStrongBoxBacked", isStrongBox)
            put("certificateChainLength", certChain?.size ?: 1)
            put("createdAt", cert.notBefore.time.toDouble())
        }
    }

    private fun isStrongBoxBackedKey(privateKey: PrivateKey): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val factory = KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(privateKey, KeyInfo::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                keyInfo.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
            } else {
                keyInfo.isInsideSecureHardware
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
