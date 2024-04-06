package com.github.enterprisebtc.nostr.core.client


import com.github.enterprisebtc.nostr.core.message.NostrMessageAdapter
import com.github.enterprisebtc.nostr.core.message.relay.RelayMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.runBlocking
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class RelayListener(
  private val label: String,
  private val connectionStateListener: ConnectionStateListener
) : WebSocketListener() {

  private val messages = MutableSharedFlow<RelayMessage>(replay = 1024)

  fun messages(): Flow<RelayMessage> = messages.asSharedFlow().buffer()

  private val relayMessageAdapter = NostrMessageAdapter.moshi.adapter(RelayMessage::class.java)

  override fun onOpen(webSocket: WebSocket, response: Response) {
    logger.info { "Socket is open. [relay=$label][response=${response.message}]" }
    connectionStateListener.update(ConnectionState.Connected)
  }

  override fun onMessage(webSocket: WebSocket, text: String) {
    logger.info { "Received $text. [relay=$label]" }
    runBlocking {
      relayMessageAdapter.fromJson(text)?.let { messages.emit(it) }
        ?: logger.warn { "Unable to handle relay message: $text. [relay=$label]" }
    }
  }

  override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    logger.info(t) { "WebSocket failure. [relay=$label]" }
    connectionStateListener.update(ConnectionState.Failing)
  }

  override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
    logger.info { "Socket is closed. [relay=$label]" }
    connectionStateListener.update(ConnectionState.Disconnected)
  }

  override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
    logger.info { "Socket is closing. [relay=$label]" }
    connectionStateListener.update(ConnectionState.Disconnecting)
  }

  companion object {
    val logger = KotlinLogging.logger {}
  }
}
