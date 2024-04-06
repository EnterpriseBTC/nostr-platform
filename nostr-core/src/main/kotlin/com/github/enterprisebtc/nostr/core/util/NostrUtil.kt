package com.github.enterprisebtc.nostr.core.util

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*

/**
 * Nostr Util bytes to hex etc
 */
object NostrUtil {
    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()

    fun bytesToHex(b: ByteArray): String {
        val hexChars = CharArray(b.size * 2)
        for (j in b.indices) {
            val v = b[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars).lowercase(Locale.getDefault())
    }

    fun hexToBytes(s: String): ByteArray {
        val len = s.length
        val buf = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            buf[i / 2] = ((((s[i].digitToIntOrNull(16) ?: (-1 shl 4)) + s[i + 1].digitToIntOrNull(16)!!) ?: -1)).toByte()
            i += 2
        }
        return buf
    }

    fun bytesFromInt(n: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(n).array()
    }

    fun bytesFromBigInteger(n: BigInteger): ByteArray {
        val b = n.toByteArray()

        if (b.size == 32) {
            return b
        } else if (b.size > 32) {
            return Arrays.copyOfRange(b, b.size - 32, b.size)
        } else {
            val buf = ByteArray(32)
            System.arraycopy(b, 0, buf, buf.size - b.size, b.size)
            return buf
        }
    }

    fun bigIntFromBytes(b: ByteArray?): BigInteger {
        return BigInteger(1, b)
    }

    @Throws(NoSuchAlgorithmException::class)
    fun sha256(b: ByteArray?): ByteArray {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        return digest.digest(b)
    }

    fun xor(b0: ByteArray, b1: ByteArray): ByteArray? {
        if (b0.size != b1.size) {
            return null
        }

        val ret = ByteArray(b0.size)
        var i = 0
        for (b in b0) {
            ret[i] = (b.toInt() xor b1[i].toInt()).toByte()
            i++
        }

        return ret
    }

    fun createRandomByteArray(len: Int): ByteArray {
        val b = ByteArray(len)
        SecureRandom().nextBytes(b)
        return b
    }
}
