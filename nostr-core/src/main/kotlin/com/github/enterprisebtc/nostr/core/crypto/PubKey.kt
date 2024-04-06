package com.github.enterprisebtc.nostr.core.crypto

import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * Nostr public key.
 */
data class PubKey(val key: ByteString) : Key {

  /** nip-19 bech32 encoded form of this key */
  val npub by lazy {
    Bech32Serde.encodeBytes("npub", key, Bech32Serde.Encoding.Bech32)
  }

  override fun encoded(): String = npub

  override fun toString() = "PubKey(key=${key.hex()})"

  /**
   * The shortened form of this key's npub. For example,
   * `npub1sdnq9yr3kwzaauhylwty6ttnum6zgcf34s0lwcythjcew8zss8fqlm45zq` becomes `sdnq9yr3:fqlm45zq`,
   * being the first 8 (after npub1) and the last 8 with a colon between.
   */
  val shortForm: String by lazy { shortBech32Regex.replace(npub, "$1:$2") }

  override fun hex(): String = key.hex()

  companion object {
    /** Create pub key from nip-19 bech32 encoded string */
    fun parse(bech32: String): PubKey {
      val (hrp, key) = Bech32Serde.decodeBytes(bech32, false)
      require(hrp == "npub") { "Unsupported encoding hrp=$hrp" }
      return PubKey(key.toByteString())
    }

    // Isolate head 8 and tail 8 of an npub
    private val shortBech32Regex = Regex("npub1([a-z\\d]{8})[a-z\\d]{42}([a-z\\d]{8})")
  }
}
