package com.github.enterprisebtc.nostr.core.message.relay

/**
 * Sent by [nip-15](https://github.com/nostr-protocol/nips/blob/master/15.md#nip-15) compliant relays when the
 * subscription has finished fetching stored events and will subsequently only send real-time events.
 */
data class EndOfStoredEvents(
  val subscriptionId: String
) : RelayMessage
