package com.github.enterprisebtc.nostr.core.client

interface ConnectionStateListener {
  fun update(newState: ConnectionState)
}
