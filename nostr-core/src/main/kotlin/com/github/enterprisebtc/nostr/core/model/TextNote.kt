package com.github.enterprisebtc.nostr.core.model

/**
 * A text note that can be published via relays. Event kind 1, as defined in
 * [nip-01](https://github.com/nostr-protocol/nips/blob/master/01.md#basic-event-kinds).
 */
data class TextNote(
  val text: String,
  override val tags: List<Tag> = emptyList()
) : EventContent {

  override val kind: Int = Companion.kind

  override fun toJsonString(): String = text

  companion object {
    const val kind = 1
  }
}
