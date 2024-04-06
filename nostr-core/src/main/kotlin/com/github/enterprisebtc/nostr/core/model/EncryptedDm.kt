package com.github.enterprisebtc.nostr.core.model

import com.github.enterprisebtc.nostr.core.crypto.CipherText
import com.github.enterprisebtc.nostr.core.crypto.PubKey
import com.github.enterprisebtc.nostr.core.crypto.SecKey
import okio.ByteString.Companion.toByteString
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * An encrypted direct message. Event kind 4, as defined in
 * [nip-04](https://github.com/nostr-protocol/nips/blob/master/04.md).
 */
data class EncryptedDm(
  val to: PubKey,
  val cipherText: CipherText,
  override val tags: List<Tag> = listOf(PubKeyTag(to)),
) : EventContent {

  constructor(from: SecKey, to: PubKey, message: String) : this(to, from.encrypt(to, message))

  override val kind: Int = EncryptedDm.kind

  override fun toJsonString() = cipherText.toString()

  /** Providing the public key of the sender and the secret key of the recipient, decode this message */
  fun decipher(from: PubKey, to: SecKey): String = cipherText.decipher(from, to)

  companion object {
    const val kind = 4
  }
}

fun SecKey.encrypt(to: PubKey, plainText: String): CipherText {
  val random = SecureRandom()
  val iv = ByteArray(16)
  random.nextBytes(iv)
  val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
  cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sharedSecretWith(to), "AES"), IvParameterSpec(iv))
  val encrypted = cipher.doFinal(plainText.toByteArray())
  return CipherText(encrypted.toByteString(), iv.toByteString())
}
