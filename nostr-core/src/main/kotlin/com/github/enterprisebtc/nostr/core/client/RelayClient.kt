package com.github.enterprisebtc.nostr.core.client

import com.github.enterprisebtc.nostr.core.message.NostrMessageAdapter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.github.enterprisebtc.nostr.core.message.relay.EventMessage
import com.github.enterprisebtc.nostr.core.message.relay.RelayMessage
import com.github.enterprisebtc.nostr.core.model.Event
import com.github.enterprisebtc.nostr.core.model.Filter
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.util.Collections
import java.util.concurrent.TimeUnit

/** Provides comms to and from a single relay */
class RelayClient(
  private val url: String,
  private val client: OkHttpClient = OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build()
) : Relay(), ConnectionStateListener {

  private val jsonListAdapter = NostrMessageAdapter.moshi.adapter(List::class.java)
  private val queuedMessages = MutableSharedFlow<String>(replay = 512)
  private val listener = RelayListener(url, this)
  private val subscriptions: MutableMap<Subscription, Set<Filter>> = Collections.synchronizedMap(mutableMapOf())

  private var _connectionState = MutableStateFlow(ConnectionState.Disconnected)
  val connectionState : StateFlow<ConnectionState> get() = _connectionState.asStateFlow() // TODO add tests

  private var messageSendingJob: Job? = null
  private var socket: WebSocket? = null

  override val relayMessages: Flow<RelayMessage> by lazy { listener.messages() }

  override fun start() {
    if (connectionState.value == ConnectionState.Disconnected) {
      logger.info { "Connecting to $url" }
      socket = client.newWebSocket(Request.Builder().url(url).build(), listener)
      _connectionState.value = ConnectionState.Connecting
    }
  }

  override fun update(newState: ConnectionState): Unit = runBlocking {
    logger.info { "$connectionState -> $newState [relay=$url]" }
    when (newState) {
        ConnectionState.Connecting -> {}
        ConnectionState.Connected -> startTalking()
        ConnectionState.Failing -> restart()
        ConnectionState.Disconnected -> connect()
        ConnectionState.Disconnecting -> stopTalking()
    }

    _connectionState.value = newState
  }

  override fun stop() {
    stopTalking()
    disconnect()
  }

  override fun send(event: Event) {
    logger.info { "Enqueuing: ${event.id} [relay=$url]" }
    send(listOf("EVENT", event))
  }

  override fun subscribe(filters: Set<Filter>, subscription: Subscription): Subscription = subscription.also {
    send(listOf<Any>("REQ", it.id).plus(filters.toList()))
    subscriptions[subscription] = filters
  }

  override fun unsubscribe(subscription: Subscription) {
    subscriptions.remove(subscription)
    send(listOf("CLOSE", subscription.id))
  }

  override val allEvents: Flow<Event> by lazy { relayMessages.filterIsInstance<EventMessage>().map { it.event } }

  private fun send(message: List<Any>) {
    queuedMessages.tryEmit(jsonListAdapter.toJson(message))
  }

  private suspend fun connect(after: Long = 0L) = coroutineScope {
    delay(after)
    logger.info { "Connecting to $url" }
    socket = client.newWebSocket(Request.Builder().url(url).build(), listener)
  }

  private suspend fun startTalking() = coroutineScope {
    messageSendingJob = CoroutineScope(Dispatchers.IO + CoroutineName(url)).launch {
      logger.info { "Starting send loop [relay=$url]" }
      queuedMessages.collect {
        logger.info { "Sending [relay=$url] $it" }
        if (socket?.send(it) != true) {
          logger.warn { "Failed to send on socket. Re-enqueueing & restarting. [relay=$url]" }
          queuedMessages.emit(it)
          restart()
        }
      }
    }
  }

  private fun restart() {
    stop()
    start()
    subscriptions.forEach { (sub, filters) ->
      subscribe(filters, sub)
    }
  }

  private fun stopTalking() {
    logger.info { "Stopping the send loop. [relay=$url]" }
    messageSendingJob?.cancel()
    messageSendingJob = null
  }

  private fun disconnect() {
    logger.info { "Disconnecting from $url" }
    if (socket?.close(1000, "Requested disconnection") == false) {
      logger.warn { "Unable to close socket cleanly. [relay=$url]" }
      socket?.cancel()
    }
  }

  companion object {
    val logger = KotlinLogging.logger {}
  }
}

enum class ConnectionState {
  Disconnected,
  Connecting,
  Connected,
  Failing,
  Disconnecting
}
