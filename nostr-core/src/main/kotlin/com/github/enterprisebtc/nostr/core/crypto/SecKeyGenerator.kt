package com.github.enterprisebtc.nostr.core.crypto

import okio.ByteString.Companion.toByteString
import org.kotlincrypto.SecureRandom

class SecKeyGenerator {

  /** Generate a new secret key */
  tailrec fun generate(): SecKey {
    val bs = ByteArray(32)
    SecureRandom().nextBytesCopyTo(bs)
    val sec = bs.toByteString()
    return if (sec.hex() > MAX_SEC) generate() else SecKey(sec)
  }

  companion object {
    private const val MAX_SEC =
      "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"
  }
}
