package com.github.enterprisebtc.nostr.core.message.relay


/** Informational notice from the relay to the client */
data class Notice(val message: String) : RelayMessage
