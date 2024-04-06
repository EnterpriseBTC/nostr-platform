package com.github.enterprisebtc.nostr.core.client

import com.github.enterprisebtc.nostr.core.message.relay.EventMessage
import com.github.enterprisebtc.nostr.core.message.relay.RelayMessage
import com.github.enterprisebtc.nostr.core.model.Event
import com.github.enterprisebtc.nostr.core.model.Filter
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.map
import okio.ByteString

data class RelaySet(
  val relays: Set<Relay>
) : Relay() {

  override fun start() = relays.forEach { it.start() }

  override fun stop() = relays.forEach { it.stop() }

  override fun send(event: Event) = relays.forEach { it.send(event) }

  override fun subscribe(filters: Set<Filter>, subscription: Subscription): Subscription = subscription.also {
    relays.forEach { it.subscribe(filters, subscription) }
  }

  override fun unsubscribe(subscription: Subscription) = relays.forEach { it.unsubscribe(subscription) }

  @OptIn(ExperimentalCoroutinesApi::class)
  override val relayMessages: Flow<RelayMessage> by lazy {
    val cache = CacheBuilder.newBuilder()
      .maximumSize(4096)
      .build<ByteString, Boolean>(CacheLoader.from { _ -> false })

    relays.map { it.relayMessages }.asFlow()
      .flattenMerge()
      .filterNot {
        it is EventMessage && cache.get(it.event.id)
      }
      .map {
        if(it is EventMessage) {
          cache.put(it.event.id, true)
        }
        it
      }
  }

  override val allEvents: Flow<Event> by lazy {
    relayMessages.filterIsInstance<EventMessage>().map { it.event }
  }
}
