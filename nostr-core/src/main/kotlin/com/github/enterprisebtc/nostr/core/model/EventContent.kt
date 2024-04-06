package com.github.enterprisebtc.nostr.core.model


import com.github.enterprisebtc.nostr.core.crypto.SecKey
import com.github.enterprisebtc.nostr.core.message.NostrMessageAdapter
import com.squareup.moshi.JsonAdapter
import okio.ByteString.Companion.encodeUtf8
import java.time.Instant
import java.time.temporal.ChronoUnit

/** A type that can be signed and converted into an Event */
interface EventContent {

  val kind: Int

  val tags: List<Tag>

  fun toJsonString(): String

  fun sign(sec: SecKey): Event {
    val createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).truncatedTo(ChronoUnit.SECONDS)
    val contentJson = toJsonString()
    val tagsJson = tags.map { it.toJsonList() }
    val elements = listOf(0, sec.pubKey.key.hex(), createdAt.epochSecond, kind, tagsJson, contentJson)
    val toJson = jsonListAdapter.toJson(elements)
    val id = toJson.encodeUtf8().sha256()
    val sig = sec.sign(id)
    return Event(id, sec.pubKey.key, createdAt, kind, tagsJson, contentJson, sig)
  }


  fun asZapRequest(): ZapRequest? =
    if (this is ZapRequest) this
    else null

  companion object {
    val jsonListAdapter: JsonAdapter<List<*>> = NostrMessageAdapter.moshi.adapter(List::class.java)
  }
}
