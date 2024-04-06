package com.github.enterprisebtc.nostr.core.model

import okio.ByteString

/**
 * An event deletion request message. Event kind 5, as defined in
 * [nip-09](https://github.com/nostr-protocol/nips/blob/master/09.md).
 */
data class EventDeletion(
  val message: String = "",
  val eventIds: Set<ByteString>,
  override val tags: List<Tag> = eventIds.map { EventTag(it) }
) : EventContent {

  override val kind: Int = EventDeletion.kind

  override fun toJsonString(): String = message

  companion object {
    const val kind = 5
  }
}
