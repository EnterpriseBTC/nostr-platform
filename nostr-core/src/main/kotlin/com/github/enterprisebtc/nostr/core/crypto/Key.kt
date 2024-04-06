package com.github.enterprisebtc.nostr.core.crypto

/** Common interface for both public and secret keys. */
interface Key {

  fun encoded(): String

  fun hex(): String

  companion object {

    /** Parse a bech32 encoded key. May return a pub or sec key depending upon what has been encoded. */
    fun parse(bech32: String): Key =
      when {
        bech32.startsWith("npub") -> PubKey.parse(bech32)
        else -> SecKey.parse(bech32)
      }
  }
}
