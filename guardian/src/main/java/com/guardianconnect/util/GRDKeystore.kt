package com.guardianconnect.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.guardianconnect.GRDConnectManager
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GRDKeystore {

    class Encryptor {
        fun encryptText(alias: String, textToEncrypt: String): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(alias))
            val ivEncoded = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
            GRDConnectManager.getSharedPrefs()?.edit()?.putString("IV" + alias, ivEncoded)?.apply()

            val dataByteArray = textToEncrypt.toByteArray(Charset.defaultCharset())
            return cipher.doFinal(dataByteArray)
        }

        fun getSecretKey(alias: String): SecretKey {
            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
            )
            return keyGenerator.generateKey()
        }
    }

    class Decryptor {
        private lateinit var keyStore: KeyStore

        fun initKeyStore() {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
        }

        fun decryptData(alias: String, encryptedData: ByteArray?): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, getIv(alias))
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(alias), spec)
            val dataArray = cipher.doFinal(encryptedData)
            return String(dataArray, Charset.defaultCharset())
        }

        fun getSecretKey(alias: String): SecretKey {
            return (keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry).secretKey
        }

        fun getIv(alias: String): ByteArray? {
            var iv: ByteArray? = null
            val ivString = GRDConnectManager.getSharedPrefs()?.getString("IV$alias", "")
            if (!ivString.isNullOrEmpty()) {
                iv = Base64.decode(ivString, Base64.DEFAULT)
            }
            return iv
        }
    }

    fun saveToKeyStore(key: String, value: String) {
        val encryptedValueToSave = Base64.encodeToString(
            encryptor.encryptText(
                key,
                value
            ), Base64.DEFAULT
        )
        GRDConnectManager.getSharedPrefsEditor()?.putString(
            key, encryptedValueToSave
        )
            ?.apply()
    }

    fun retrieveFromKeyStore(key: String): String? {
        val encryptedValue =
            GRDConnectManager.getSharedPrefs()?.getString(key, "")
        var returnString: String? = null

        try {
            returnString = if (!encryptedValue.isNullOrEmpty()) {
                decryptor.initKeyStore()
                val decryptedValue =
                    decryptor.decryptData(
                        key,
                        Base64.decode(
                            encryptedValue,
                            Base64.DEFAULT
                        )
                    )
                decryptedValue
            } else {
                null
            }
        } catch (e: Exception) {
            e.message?.let { Log.e("GRDKeystore", it) }
            GRDConnectManager.getSharedPrefsEditor()?.remove(key)
        }
        return returnString
    }

    fun removePEToken() {
        GRDConnectManager.getSharedPrefsEditor()?.remove(Constants.GRD_PE_TOKEN)?.apply()
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private val decryptor = Decryptor()
        private val encryptor = Encryptor()
        val instance = GRDKeystore()
    }
}