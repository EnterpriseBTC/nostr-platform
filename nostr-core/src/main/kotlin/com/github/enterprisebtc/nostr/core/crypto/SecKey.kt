package com.github.enterprisebtc.nostr.core.crypto

import fr.acinq.secp256k1.Hex
import fr.acinq.secp256k1.Secp256k1
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * Nostr secret key.
 */
data class SecKey(val key: ByteString) : Key {
  override fun toString() = "SecKey(███)"

  /** nip-19 bech32 encoded form of this key */
  val nsec by lazy {
    Bech32Serde.encodeBytes("nsec", key, Bech32Serde.Encoding.Bech32)
  }

  override fun encoded(): String = nsec

  override fun hex(): String = key.hex()

  /** the public key derived from this secret key */
  val pubKey by lazy {
    PubKey(Secp256k1.pubKeyCompress(Secp256k1.pubkeyCreate(key.toByteArray())).copyOfRange(1, 33).toByteString())
  }

  /** sign any arbitrary payload with this key */
  fun sign(payload: ByteString): ByteString =
    Secp256k1.signSchnorr(payload.toByteArray(), key.toByteArray(), null).toByteString()

  /** Find the point of shared secret between this sec key and a pub key */
  fun sharedSecretWith(pub: PubKey): ByteArray =
    Secp256k1.pubKeyTweakMul(
      pubkey = Hex.decode("02") + pub.key.toByteArray(),
      tweak = key.toByteArray()
    ).copyOfRange(1, 33)

  /** Generate cipher text of the provided plain text, intended for the provided pub key */


  companion object {
    /** Create secret key from nip-19 bech32 encoded string */
    fun parse(bech32: String): SecKey {
      val (hrp, key) = Bech32Serde.decodeBytes(bech32, false)
      require(hrp == "nsec") { "Unsupported encoding hrp=$hrp" }
      return SecKey(key.toByteString())
    }
  }
}
