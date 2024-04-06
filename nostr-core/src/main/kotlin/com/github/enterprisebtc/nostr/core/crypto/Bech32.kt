package com.github.enterprisebtc.nostr.core.crypto

import com.github.enterprisebtc.nostr.core.util.NostrException
import com.github.enterprisebtc.nostr.core.util.NostrUtil
import java.io.UnsupportedEncodingException
import java.security.NoSuchAlgorithmException
import java.util.Locale

/**
 * Implementation of the Bech32 encoding.
 *
 * See [BIP350](https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki)
 * and [BIP173](https://github.com/bitcoin/bips/blob/master/bip-0173.mediawiki)
 * for details.
 */
object Bech32 {

    /**
     * The Bech32 character set for encoding.
     */
    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"

    /**
     * The Bech32 character set for decoding.
     */
    private val CHARSET_REV = byteArrayOf(
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1,
        -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
        1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1
    )

    private const val BECH32_CONST = 1
    private const val BECH32M_CONST = 0x2bc830a3

    enum class Encoding {
        BECH32, BECH32M
    }

    data class Bech32Data(val encoding: Encoding, val hrp: String, val data: ByteArray)

    /**
     * Encodes a public key to Bech32 format.
     *
     * @param hrp The human-readable part of the Bech32 string.
     * @param hexPubKey The public key in hexadecimal format.
     * @return The Bech32-encoded public key.
     * @throws NoSuchAlgorithmException If the required cryptographic algorithm is not available.
     * @throws NostrException If an error occurs during the encoding process.
     * @throws UnsupportedEncodingException If the required character encoding is not supported.
     */
    @Throws(NoSuchAlgorithmException::class, NostrException::class, UnsupportedEncodingException::class)
    fun toBech32(hrp: String, hexPubKey: String): String {
        var data = NostrUtil.hexToBytes(hexPubKey)
        data = convertBits(data, 8, 5, true)
        return encode(Encoding.BECH32, hrp, data)
    }

    /**
     * Decodes a Bech32-encoded public key.
     *
     * @param strBech32 The Bech32-encoded public key.
     * @return The decoded public key in hexadecimal format.
     * @throws NostrException If an error occurs during the decoding process.
     */
    @Throws(NostrException::class)
    fun fromBech32(strBech32: String): String {
        var data = decode(strBech32).data
        data = convertBits(data, 5, 8, true)
        // Remove trailing bit
        data = data.copyOfRange(0, data.size - 1)
        return NostrUtil.bytesToHex(data)
    }

    /**
     * Encodes a Bech32 string.
     *
     * @param bech32 The Bech32 data to encode.
     * @return The encoded Bech32 string.
     * @throws NostrException If an error occurs during the encoding process.
     */
    @Throws(NostrException::class)
    fun encode(bech32: Bech32Data): String {
        return encode(bech32.encoding, bech32.hrp, bech32.data)
    }

    /**
     * Encodes a Bech32 string.
     *
     * @param encoding The Bech32 encoding scheme.
     * @param hrp The human-readable part of the Bech32 string.
     * @param values The data values to encode.
     * @return The encoded Bech32 string.
     * @throws NostrException If an error occurs during the encoding process.
     */
    @Throws(NostrException::class)
    fun encode(encoding: Encoding, hrp: String, values: ByteArray): String {
        require(hrp.isNotEmpty()) { "Human-readable part is too short" }
        val hrpLower = hrp.lowercase(Locale.ROOT)
        val checksum = createChecksum(encoding, hrpLower, values)
        val combined = ByteArray(values.size + checksum.size)
        System.arraycopy(values, 0, combined, 0, values.size)
        System.arraycopy(checksum, 0, combined, values.size, checksum.size)
        val sb = StringBuilder(hrpLower.length + 1 + combined.size)
        sb.append(hrpLower)
        sb.append('1')
        for (b in combined) {
            sb.append(CHARSET[b.toInt()])
        }
        return sb.toString()
    }

    /**
     * Decodes a Bech32 string.
     *
     * @param str The Bech32 string to decode.
     * @return The decoded Bech32 data.
     * @throws NostrException If an error occurs during the decoding process.
     */
    @Throws(NostrException::class)
    fun decode(str: String): Bech32Data {
        var lower = false
        var upper = false
        require(str.length >= 8) { "Input too short: ${str.length}" }
        for (i in str.indices) {
            val c = str[i]
            require(c.code in 33..126) { "Invalid Character $c, $i" }
            if (c in 'a'..'z') {
                require(!upper) { "Invalid Character $c, $i" }
                lower = true
            }
            if (c in 'A'..'Z') {
                require(!lower) { "Invalid Character $c, $i" }
                upper = true
            }
        }
        val pos = str.lastIndexOf('1')
        require(pos >= 1) { "Missing human-readable part" }
        val dataPartLength = str.length - 1 - pos
        require(dataPartLength >= 6) { "Data part too short: $dataPartLength)" }
        val values = ByteArray(dataPartLength)
        for (i in 0 until dataPartLength) {
            val c = str[i + pos + 1]
            require(CHARSET_REV[c.code] != (-1).toByte()) { "Invalid Character $c, ${i + pos + 1}" }
            values[i] = CHARSET_REV[c.code]
        }
        val hrp = str.substring(0, pos).lowercase(Locale.ROOT)
        val encoding = verifyChecksum(hrp, values)
        checkNotNull(encoding) { "InvalidChecksum" }
        return Bech32Data(encoding, hrp, values.copyOfRange(0, values.size - 6))
    }

    /**
     * Finds the polynomial with value coefficients mod the generator as 30-bit.
     *
     * @param values The polynomial coefficients.
     * @return The polynomial value.
     */
    private fun polymod(values: ByteArray): Int {
        var c = 1
        for (v_i in values) {
            val c0 = (c ushr 25) and 0xff
            c = ((c and 0x1ffffff) shl 5) xor (v_i.toInt() and 0xff)
            if ((c0 and 1) != 0) {
                c = c xor 0x3b6a57b2
            }
            if ((c0 and 2) != 0) {
                c = c xor 0x26508e6d
            }
            if ((c0 and 4) != 0) {
                c = c xor 0x1ea119fa
            }
            if ((c0 and 8) != 0) {
                c = c xor 0x3d4233dd
            }
            if ((c0 and 16) != 0) {
                c = c xor 0x2a1462b3
            }
        }
        return c
    }

    /**
     * Expands a HRP for use in checksum computation.
     *
     * @param hrp The human-readable part of the Bech32 string.
     * @return The expanded HRP.
     */
    private fun expandHrp(hrp: String): ByteArray {
        val hrpLength = hrp.length
        val ret = ByteArray(hrpLength * 2 + 1)
        for (i in 0 until hrpLength) {
            val c = hrp[i].code and 0x7f // Limit to standard 7-bit ASCII
            ret[i] = ((c ushr 5) and 0x07).toByte()
            ret[i + hrpLength + 1] = (c and 0x1f).toByte()
        }
        ret[hrpLength] = 0
        return ret
    }

    /**
     * Verifies a Bech32 checksum.
     *
     * @param hrp The human-readable part of the Bech32 string.
     * @param values The data values.
     * @return The encoding scheme if the checksum is valid, or null if the checksum is invalid.
     */
    private fun verifyChecksum(hrp: String, values: ByteArray): Encoding? {
        val hrpExpanded = expandHrp(hrp)
        val combined = ByteArray(hrpExpanded.size + values.size)
        System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.size)
        System.arraycopy(values, 0, combined, hrpExpanded.size, values.size)
        val check = polymod(combined)
        return when (check) {
            BECH32_CONST -> Encoding.BECH32
            BECH32M_CONST -> Encoding.BECH32M
            else -> null
        }
    }

    /**
     * Creates a Bech32 checksum.
     *
     * @param encoding The Bech32 encoding scheme.
     * @param hrp The human-readable part of the Bech32 string.
     * @param values The data values.
     * @return The checksum bytes.
     */
    private fun createChecksum(encoding: Encoding, hrp: String, values: ByteArray): ByteArray {
        val hrpExpanded = expandHrp(hrp)
        val enc = ByteArray(hrpExpanded.size + values.size + 6)
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.size)
        System.arraycopy(values, 0, enc, hrpExpanded.size, values.size)
        val mod = polymod(enc) xor if (encoding == Encoding.BECH32) BECH32_CONST else BECH32M_CONST
        val ret = ByteArray(6)
        for (i in 0 until 6) {
            ret[i] = ((mod ushr (5 * (5 - i))) and 31).toByte()
        }
        return ret
    }

    /**
     * Converts data from one bit width to another.
     *
     * @param data The data to convert.
     * @param fromWidth The current bit width of the data.
     * @param toWidth The desired bit width of the data.
     * @param pad Whether to pad the output with zero bits.
     * @return The converted data, or null if the conversion is not possible.
     */
    private fun convertBits(data: ByteArray, fromWidth: Int, toWidth: Int, pad: Boolean): ByteArray {
        var acc = 0
        var bits = 0
        val result = mutableListOf<Byte>()
        for (i in data.indices) {
            val value = data[i].toInt() and 0xff and ((1 shl fromWidth) - 1)
            acc = (acc shl fromWidth) or value
            bits += fromWidth
            while (bits >= toWidth) {
                bits -= toWidth
                result.add(((acc shr bits) and ((1 shl toWidth) - 1)).toByte())
            }
        }
        if (pad) {
            if (bits > 0) {
                result.add(((acc shl (toWidth - bits)) and ((1 shl toWidth) - 1)).toByte())
            }
        } else if (bits == fromWidth || ((acc shl (toWidth - bits)) and ((1 shl toWidth) - 1)) != 0) {
            return byteArrayOf()
        }
        return result.toByteArray()
    }
}