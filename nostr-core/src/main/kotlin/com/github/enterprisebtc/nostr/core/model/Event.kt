package com.github.enterprisebtc.nostr.core.model

import com.github.enterprisebtc.nostr.core.crypto.CipherText
import com.github.enterprisebtc.nostr.core.message.NostrMessageAdapter
import com.squareup.moshi.Json
import fr.acinq.secp256k1.Secp256k1
import okio.ByteString
import java.time.Instant

/** The primitive type understood by relays, as per nip-01 */
data class Event(
  val id: ByteString,
  @Json(name = "pubkey")
  val pubKey: ByteString,
  @Json(name = "created_at")
  val createdAt: Instant,
  val kind: Int,
  val tags: List<List<String>>,
  val content: String,
  val sig: ByteString
) {

  /**
   * Valid is `true` if the event has a valid signature.
   */
  val validSignature: Boolean by lazy {
    Secp256k1.verifySchnorr(sig.toByteArray(), id.toByteArray(), pubKey.toByteArray())
  }

  /**
   * Deserialise the `content` string into an instance of `EventContent` that corresponds with the event `kind`.
   */
  fun content(): EventContent {
    val tags = tags.map { Tag.parseRaw(it) }
    val taggedPubKeys by lazy { tags.filterIsInstance<PubKeyTag>().map { it.pubKey } }
    val taggedEventIds by lazy { tags.filterIsInstance<EventTag>().map { it.eventId } }
    return when (this.kind) {
      TextNote.kind -> TextNote(content, tags)
      EncryptedDm.kind -> EncryptedDm(taggedPubKeys.first(), CipherText.parse(content), tags)
      EventDeletion.kind -> EventDeletion(content, taggedEventIds.toSet())
      Reaction.kind -> Reaction.from(content, taggedEventIds.last(), taggedPubKeys.last(), tags)
      ZapRequest.kind -> {
        val relays = tags.filterIsInstance<RelaysTag>().first().relays
        val amount = tags.filterIsInstance<AmountTag>().firstOrNull()?.amount
        val lnUrl = tags.filterIsInstance<LnUrlTag>().firstOrNull()?.lnurl
        ZapRequest(content, relays, amount, lnUrl, taggedPubKeys.first(), taggedEventIds.firstOrNull())
      }

      ZapReceipt.kind -> {
        val preimage = tags.filterIsInstance<PreimageTag>().firstOrNull()?.preimage
        val bolt11 = tags.filterIsInstance<Bolt11Tag>().firstOrNull()?.bolt11
        val description = tags.filterIsInstance<ZapReceiptDescriptionTag>().firstOrNull()?.description

        requireNotNull(bolt11 ) {
          "ZapReceipt event must have a bolt11 tag"
        }

        requireNotNull(description) {
          "ZapReceipt event must have a description tag"
        }

        ZapReceipt(taggedPubKeys.first(), taggedEventIds.firstOrNull(), bolt11, description, preimage)
      }

      else -> adapters[this.kind]?.fromJson(content)!!.copy(tags = tags)
    }
  }

  fun toJson(): String {
    return NostrMessageAdapter.moshi.adapter(Event::class.java).toJson(this)
  }

  companion object {
    private val adapters = mapOf(
      UserMetaData.kind to NostrMessageAdapter.moshi.adapter(UserMetaData::class.java),
    )

    fun fromJson(json: String): Event? {
      return NostrMessageAdapter.moshi.adapter(Event::class.java).fromJson(json)
    }
  }
}
