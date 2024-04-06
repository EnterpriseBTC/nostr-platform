package com.github.enterprisebtc.nostr.core.model

import com.github.enterprisebtc.nostr.core.crypto.PubKey
import okio.ByteString

/**
 * Zap request. Event kind 9734, as defined in
 * [nip-57](https://github.com/nostr-protocol/nips/blob/master/57.md).
 */
data class ZapRequest(
  val content: String,
  val relays: List<String>,
  val amount: Long?,
  val lnurl: String?,
  val to: PubKey,
  val eventId: ByteString?,
  override val tags: List<Tag> = listOfNotNull(
    RelaysTag(relays),
    amount?.let(::AmountTag),
    lnurl?.let(::LnUrlTag),
    PubKeyTag(to),
    eventId?.let(::EventTag)
  )
) : EventContent {
  override val kind = Companion.kind

  override fun toJsonString() = content

  companion object {
    const val kind = 9734
  }
}
