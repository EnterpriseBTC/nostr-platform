package com.github.enterprisebtc.nostr.core.model

import com.github.enterprisebtc.nostr.core.message.NostrMessageAdapter
import com.squareup.moshi.Json

/**
 * User metadata (profile). Event kind 0, as defined in
 * [nip-01](https://github.com/nostr-protocol/nips/blob/master/01.md#basic-event-kinds).
 */
data class UserMetaData(
  val name: String? = null,
  val about: String? = null,
  val picture: String? = null,
  val nip05: String? = null,
  val banner: String? = null,
  @Json(name = "display_name")
  val displayName: String? = null,
  val website: String? = null,
  override val tags: List<Tag> = emptyList(),
) : EventContent {

  override val kind: Int = UserMetaData.kind

  override fun toJsonString(): String = adapter.toJson(this)

  companion object {
    const val kind: Int = 0
    private val adapter by lazy { NostrMessageAdapter.moshi.adapter(UserMetaData::class.java) }
  }
}
