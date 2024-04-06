package com.github.enterprisebtc.nostr.core.model

import com.github.enterprisebtc.nostr.core.crypto.PubKey
import okio.ByteString
import okio.ByteString.Companion.decodeHex

sealed interface Tag {
  fun toJsonList(): List<String>

  companion object {
    fun parseRaw(strings: List<String>): Tag {
      require(strings.size >= 2) { "Invalid tag format: $strings" }
      val (tag, value) = strings
      val values = strings.drop(1)
      return when (tag) {
        "e" -> EventTag(value.decodeHex())
        "p" -> PubKeyTag(PubKey(value.decodeHex()))
        "t" -> HashTag(value)
        "amount" -> AmountTag(value.toLong())
        "lnurl" -> LnUrlTag(value)
        "relays" -> RelaysTag(values)
        "bolt11" -> Bolt11Tag(value)
        "preimage" -> PreimageTag(value.decodeHex())
        "description" -> {
          val event = Event.fromJson(value)
          require(event != null) { "Invalid tag format: $strings" }

          ZapReceiptDescriptionTag(event)
        }

        else -> throw IllegalArgumentException("Invalid tag format: $strings")
      }
    }
  }
}

data class EventTag(val eventId: ByteString) : Tag {
  override fun toJsonList() = listOf("e", eventId.hex())
}

data class PubKeyTag(val pubKey: PubKey) : Tag {
  override fun toJsonList() = listOf("p", pubKey.hex())
}

data class HashTag(val label: String) : Tag {
  override fun toJsonList() = listOf("t", label)
}

data class RelaysTag(val relays: List<String>) : Tag {
  override fun toJsonList() = listOf("relays") + relays
}

data class AmountTag(val amount: Long) : Tag {
  override fun toJsonList() = listOf("amount", amount.toString())
}

data class LnUrlTag(val lnurl: String) : Tag {
  override fun toJsonList() = listOf("lnurl", lnurl)
}

data class Bolt11Tag(val bolt11: String) : Tag {
  override fun toJsonList() = listOf("bolt11", bolt11)
}

data class PreimageTag(val preimage: ByteString) : Tag {
  override fun toJsonList() = listOf("preimage", preimage.hex())
}

data class ZapReceiptDescriptionTag(val description: Event) : Tag {
  override fun toJsonList(): List<String> = listOf(
    "description", description.toJson(),
  )
}
