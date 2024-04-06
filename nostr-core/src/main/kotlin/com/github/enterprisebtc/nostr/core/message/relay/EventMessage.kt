package com.github.enterprisebtc.nostr.core.message.relay

import com.github.enterprisebtc.nostr.core.model.Event


/** Every event provided by the relay is associated with a subscription. This type binds both. */
data class EventMessage(
  val subscriptionId: String,
  val event: Event
) : RelayMessage
