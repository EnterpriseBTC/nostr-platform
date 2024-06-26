package com.github.enterprisebtc.nostr.core.crypto

import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import java.lang.NullPointerException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Encrypted text. Contains the ciphertext bytes and an initialisation vector to be used during the AES decryption. */
data class CipherText(
  val cipherText: ByteString,
  val iv: ByteString
) {
  override fun toString() = "${cipherText.base64()}?iv=${iv.base64()}"

  /**
   * Find the plain text message from this cipher text. It is necessary to provide the PubKey of the author, and the
   * SecKey of the recipient.
   */
  fun decipher(from: PubKey, to: SecKey): String {
    val sharedSecret = to.sharedSecretWith(from)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(sharedSecret, "AES"), IvParameterSpec(iv.toByteArray()))
    return String(cipher.doFinal(cipherText.toByteArray()))
  }

  companion object {

    /** Parse a string value of ciphertext + initialisation vector into a CipherText instance. */
    fun parse(value: String): CipherText {
      val parts = value.split("?iv=")
      require(parts.size == 2) { "Invalid cipherText (should be \"\${cipher}?iv=\${iv}\"): $value" }
      return try {
        CipherText(parts[0].decodeBase64()!!, parts[1].decodeBase64()!!)
      } catch (e: NullPointerException) {
        throw IllegalArgumentException("Invalid cipherText (bad base64): $value")
      }
    }
  }
}
