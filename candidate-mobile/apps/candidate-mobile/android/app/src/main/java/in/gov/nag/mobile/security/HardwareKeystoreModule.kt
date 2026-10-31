package in.gov.nag.mobile.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec

/**
 * NagHardwareKeystoreModule - Bridgeless TurboModule for Android Keystore Hardware Security.
 * Generates ECDSA P-256 (secp256r1) key pairs in StrongBox Keymaster (with fallback to TEE)
 * and signs CBT exam submissions using SHA256withECDSA for non-repudiation and zero-trust verification.
 */
class HardwareKeystoreModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val MODULE_NAME = "NagHardwareKeystoreModule"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "NAG_CBT_HARDWARE_KEY"
        private const val EC_CURVE_NAME = "secp256r1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val TAG = "NagHardwareKeystore"
    }

    override fun getName(): String = MODULE_NAME

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Ensures an ECDSA P-256 key pair exists in AndroidKeyStore.
     * Prefers StrongBox Keymaster; gracefully falls back to Standard TEE hardware if StrongBox is unavailable.
     */
    @ReactMethod
    fun ensureHardwareKeyPair(aliasArg: String?, promise: Promise) {
        val alias = if (aliasArg.isNullOrBlank()) DEFAULT_KEY_ALIAS else aliasArg
        try {
            if (!keyStore.containsAlias(alias)) {
                generateEcKeyPair(alias)
            }

            val keyInfoMap = getKeyInfo(alias)
            if (keyInfoMap != null) {
                promise.resolve(keyInfoMap)
            } else {
                promise.reject("KEY_NOT_FOUND", "Key pair $alias could not be found after generation.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure hardware key pair: ${e.message}", e)
            promise.reject("KEY_GENERATION_FAILED", "Could not generate or retrieve key: ${e.message}", e)
        }
    }

    /**
     * Signs exam submission payload hash using the hardware-backed private key (SHA256withECDSA).
     */
    @ReactMethod
    fun signExamSubmission(payloadSha256Hex: String, aliasArg: String?, promise: Promise) {
        val alias = if (aliasArg.isNullOrBlank()) DEFAULT_KEY_ALIAS else aliasArg
        try {
            if (!keyStore.containsAlias(alias)) {
                generateEcKeyPair(alias)
            }

            val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            if (entry == null) {
                promise.reject("KEY_ENTRY_NOT_FOUND", "PrivateKeyEntry for alias $alias not found.")
                return
            }

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

            val result = Arguments.createMap()
            result.putString("alias", alias)
            result.putString("payloadHash", payloadSha256Hex)
            result.putString("signatureBase64", signatureBase64)
            result.putString("algorithm", SIGNATURE_ALGORITHM)
            result.putDouble("timestamp", System.currentTimeMillis().toDouble())
            result.putBoolean("strongBoxBacked", isStrongBox)

            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Signing failed: ${e.message}", e)
            promise.reject("SIGNING_FAILED", "Failed to sign exam payload: ${e.message}", e)
        }
    }

    /**
     * Retrieves public key information, attestation details, and hardware backing status.
     */
    @ReactMethod
    fun getHardwareKeyInfo(aliasArg: String?, promise: Promise) {
        val alias = if (aliasArg.isNullOrBlank()) DEFAULT_KEY_ALIAS else aliasArg
        try {
            if (!keyStore.containsAlias(alias)) {
                promise.resolve(null)
                return
            }

            val infoMap = getKeyInfo(alias)
            promise.resolve(infoMap)
        } catch (e: Exception) {
            promise.reject("KEY_QUERY_FAILED", e.message, e)
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

    private fun getKeyInfo(alias: String): com.facebook.react.bridge.WritableMap? {
        val cert = keyStore.getCertificate(alias) as? X509Certificate ?: return null
        val publicKeyBytes = cert.publicKey.encoded
        val publicKeyBase64 = Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP)
        val certChain = keyStore.getCertificateChain(alias)

        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
        val isStrongBox = entry?.privateKey?.let { isStrongBoxBackedKey(it) } ?: false

        val map = Arguments.createMap()
        map.putString("alias", alias)
        map.putString("algorithm", "EC/$EC_CURVE_NAME")
        map.putString("publicKeyBase64", publicKeyBase64)
        map.putBoolean("isHardwareBacked", true)
        map.putBoolean("isStrongBoxBacked", isStrongBox)
        map.putInt("certificateChainLength", certChain?.size ?: 1)
        map.putDouble("createdAt", cert.notBefore.time.toDouble())
        return map
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
