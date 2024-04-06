package com.github.enterprisebtc.nostr.core.model

import com.github.enterprisebtc.nostr.core.crypto.PubKey
import com.squareup.moshi.Json
import okio.ByteString
import java.time.Instant
import kotlin.time.Duration.Companion.hours

/**
 * A subscription filter, as defined in
 * [nip-01](https://github.com/nostr-protocol/nips/blob/master/01.md#from-client-to-relay-sending-events-and-creating-subscriptions).
 */
data class Filter(
  val ids: Set<String>? = null,
  val since: Instant? = null,
  val authors: Set<String>? = null,
  val kinds: Set<Int>? = null,
  @Json(name = "#e")
  val eTags: Set<String>? = null,
  @Json(name = "#p")
  val pTags: Set<String>? = null,
  @Json(name = "#t")
  val tTags: Set<String>? = null,
  val limit: Int? = null
) {

  /** Add additional author public keys to this filter */
  fun plusAuthors(vararg authors: PubKey) = copy(
    authors = authors.map { it.key.hex() }.plus(this.authors ?: emptySet()).toSet()
  )

  companion object {

    /** All text notes. */
    val globalFeedNotes = Filter(
      since = Instant.now().minusSeconds(12.hours.inWholeSeconds),
      kinds = setOf(TextNote.kind),
      limit = 500
    )

    /** Text notes authored by the given public key only */
    fun userNotes(author: PubKey, since: Instant = Instant.EPOCH) = userNotes(
      authors = setOf(author),
      since = since
    )

    /** Text notes authored by any of the public keys provided */
    fun userNotes(authors: Set<PubKey>, since: Instant = Instant.EPOCH) = Filter(
      since = since,
      authors = authors.map { it.key.hex() }.toSet(),
      kinds = setOf(TextNote.kind),
      limit = 500
    )

    /** Direct messages authored by the provided public key */
    fun directMessages(pubKey: PubKey, since: Instant = Instant.EPOCH) = Filter(
      since = since,
      kinds = setOf(EncryptedDm.kind),
      pTags = setOf(pubKey.key.hex())
    )

    /** MetaData of the given public key */
    fun userMetaData(pubKey: PubKey, since: Instant = Instant.EPOCH) = Filter(
      since = since,
      kinds = setOf(UserMetaData.kind),
      authors = setOf(pubKey.key.hex())
    )

    fun hashTagNotes(
      hashtags: Set<HashTag>,
      since: Instant = Instant.EPOCH,
      limit: Int = 500,
    ) = Filter(
      since = since,
      kinds = setOf(TextNote.kind),
      tTags = hashtags.map { it.label }.toSet(),
      limit = limit
    )

    /**
     * Reactions authored by the given public key
     * and/or associated with the given event
     * and/or in response to the given public key.
     */
    fun reactions(
      author: PubKey? = null,
      eventId: ByteString? = null,
      eventAuthor: PubKey? = null,
      since: Instant = Instant.EPOCH
    ) = Filter(
      since = since,
      kinds = setOf(Reaction.kind),
      authors = author?.let { setOf(it.key.hex()) },
      pTags = eventAuthor?.let { setOf(it.key.hex()) },
      eTags = eventId?.let { setOf(it.hex()) },
    )
  }
}
