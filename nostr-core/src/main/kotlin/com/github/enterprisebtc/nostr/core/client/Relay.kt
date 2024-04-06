package com.github.enterprisebtc.nostr.core.client

import com.github.enterprisebtc.nostr.core.message.relay.RelayMessage
import com.github.enterprisebtc.nostr.core.model.*

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import java.util.*

abstract class Relay {

  /** Begin sending and receiving events with this Relay */
  abstract fun start()

  /** Stop sending and receiving events with this Relay */
  abstract fun stop()

  /** Queue an event to be sent (potentially immediately) */
  abstract fun send(event: Event)

  /** Create a new subscription with exactly one filter */
  fun subscribe(
    filter: Filter,
    subscription: Subscription = Subscription(UUID.randomUUID().toString())
  ) = subscribe(setOf(filter), subscription)

  /** Create a new subscription with zero to many filters */
  abstract fun subscribe(
    filters: Set<Filter>,
    subscription: Subscription = Subscription(UUID.randomUUID().toString())
  ): Subscription

  /** Unsubscribe from a subscription */
  abstract fun unsubscribe(subscription: Subscription)

  /** All messages transmitted by this relay for our active subscriptions */
  abstract val relayMessages : Flow<RelayMessage>

  /** The subset of [RelayMessage] that only contain messages of type [EventMessage] */
  abstract val allEvents: Flow<Event>

  /** The subset of [allEvents] that are of type [TextNote] */
  val notes: Flow<Event> by lazy { allEvents.filter { it.kind == TextNote.kind } }

  /** The subset of [allEvents] that are of type [EncryptedDm] */
  val directMessages: Flow<Event> by lazy { allEvents.filter { it.kind == EncryptedDm.kind } }

  /** The subset of [allEvents] that are of type [UserMetaData] */
  val userMetaData: Flow<Event> by lazy { allEvents.filter { it.kind == UserMetaData.kind } }

  /** The subset of [allEvents] that are of type [Reaction] */
  val reactions: Flow<Event> by lazy { allEvents.filter { it.kind == Reaction.kind } }
}
