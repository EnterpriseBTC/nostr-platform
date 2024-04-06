package com.github.enterprisebtc.nostr.core.message

import com.github.enterprisebtc.nostr.core.message.relay.*
import com.github.enterprisebtc.nostr.core.model.Event
import com.github.enterprisebtc.nostr.core.model.Tag
import com.github.enterprisebtc.nostr.core.model.TextNote
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import java.time.Instant

/** Moshi adapters for JSON types in the protocol. */
class NostrMessageAdapter {

  // === RelayMessages

  @FromJson
  fun relayMessageFromJson(
    reader: JsonReader,
    eoseDelegate: JsonAdapter<EndOfStoredEvents>,
    commandDelegate: JsonAdapter<CommandResult>,
    noticeDelegate: JsonAdapter<Notice>,
    eventDelegate: JsonAdapter<EventMessage>
  ): RelayMessage{
    val peekyReader = reader.peekJson()
    peekyReader.beginArray()
    return when (val messageType = peekyReader.nextString()) {
      "EOSE" -> eoseDelegate.fromJson(reader)!!
      "OK" -> commandDelegate.fromJson(reader)!!
      "EVENT" -> eventDelegate.fromJson(reader)!!
      "NOTICE" -> noticeDelegate.fromJson(reader)!!
      else -> error("Unsupported message type: $messageType")
    }
  }

  @ToJson
  fun relayMessageToJson(message: RelayMessage) = when (message) {
    is CommandResult -> commandResultToJson(message)
    is EventMessage -> eventMessageToJson(message)
    is Notice -> noticeToJson(message)
    is EndOfStoredEvents -> eoseToJson(message)
    else -> error("Unsupported message type: ${message::class.qualifiedName}")
  }

  @FromJson
  fun eoseFromJson(reader: JsonReader): EndOfStoredEvents {
    reader.beginArray()
    require(reader.nextString() == "EOSE")
    val eose = EndOfStoredEvents(reader.nextString())
    reader.endArray()
    return eose
  }

  @ToJson
  fun eoseToJson(eose: EndOfStoredEvents) = listOf("EOSE", eose.subscriptionId)

  @FromJson
  fun commandResultFromJson(reader: JsonReader): CommandResult {
    reader.beginArray()
    require(reader.nextString() == "OK")
    val result = CommandResult(
      eventId = reader.nextString().decodeHex(),
      success = reader.nextBoolean(),
      message = if (reader.hasNext()) reader.nextString() else null
    )
    reader.endArray()
    return result
  }

  @ToJson
  fun commandResultToJson(cr: CommandResult) =
    listOfNotNull("OK", cr.eventId.hex(), cr.success, cr.message)

  @FromJson
  fun noticeFromJson(reader: JsonReader): Notice {
    reader.beginArray()
    require(reader.nextString() == "NOTICE")
    val notice = Notice(reader.nextString())
    reader.endArray()
    return notice
  }

  @ToJson
  fun noticeToJson(notice: Notice) = listOf("NOTICE", notice.message)

  @FromJson
  fun eventMessageFromJson(
    reader: JsonReader,
    delegate: JsonAdapter<Event>
  ): EventMessage {
    reader.beginArray()
    require(reader.nextString() == "EVENT")
    val subscriptionId = reader.nextString()
    val event = delegate.fromJson(reader)!!
    reader.endArray()
    return EventMessage(subscriptionId, event)
  }

  @ToJson
  fun eventMessageToJson(eventMessage: EventMessage) = listOf("EVENT", eventMessage.subscriptionId, eventMessage.event)

  @FromJson
  fun textNoteFromJson(text: String) = TextNote(text)

  @ToJson
  fun textNoteToJson(note: TextNote) = note.text

  @FromJson
  fun tagFromJson(strings: List<String>) = Tag.parseRaw(strings)

  @ToJson
  fun tagToJson(tag: Tag) = tag.toJsonList()

  // === primitives

  @FromJson
  fun byteStringFromJson(s: String): ByteString = s.decodeHex()

  @ToJson
  fun byteStringToJson(b: ByteString): String = b.hex()

  @FromJson
  fun instantFromJson(seconds: Long): Instant = Instant.ofEpochSecond(seconds)

  @ToJson
  fun instantToJson(i: Instant): Long = i.epochSecond

  companion object {
    val moshi = Moshi.Builder()
      .add(NostrMessageAdapter())
      .addLast(KotlinJsonAdapterFactory())
      .build()
  }
}
