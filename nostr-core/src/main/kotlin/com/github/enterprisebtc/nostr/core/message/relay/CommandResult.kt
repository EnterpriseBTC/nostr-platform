package com.github.enterprisebtc.nostr.core.message.relay

import okio.ByteString

/**
 * Result of issuing an event to a relay, as per
 * [nip-20](https://github.com/nostr-protocol/nips/blob/master/20.md#nip-20).
 */
data class CommandResult(
  val eventId: ByteString,
  val success: Boolean,
  val message: String?
) : RelayMessage
