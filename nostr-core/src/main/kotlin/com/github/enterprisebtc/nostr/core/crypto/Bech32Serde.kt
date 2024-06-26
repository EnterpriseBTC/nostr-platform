package com.github.enterprisebtc.nostr.core.crypto

import okio.ByteString
import kotlin.jvm.JvmStatic

object Bech32Serde {

  private const val alphabet: String = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
  private val charToFive: Array<Byte> = run {
    val t = Array<Byte>(255) { -1 }
    for (i in 0..alphabet.lastIndex) {
      t[alphabet[i].code] = i.toByte()
    }
    t
  }

  /**
   * @param hrp human readable prefix
   * @param data data to encode
   * @param encoding encoding to use (bech32 or bech32m)
   * @return hrp + data encoded as a Bech32 string
   */
  @JvmStatic
  fun encodeBytes(hrp: String, data: ByteString, encoding: Encoding): String =
    encode(hrp, eight2five(data), encoding)

  /**
   * @param hrp human readable prefix
   * @param int5s 5-bit data
   * @param encoding encoding to use (bech32 or bech32m)
   * @return hrp + data encoded as a Bech32 string
   */
  @JvmStatic
  private fun encode(hrp: String, int5s: Array<Byte>, encoding: Encoding): String {
    require(hrp.lowercase() == hrp || hrp.uppercase() == hrp) { "mixed case strings are not valid bech32 prefixes" }
    val data = int5s.toByteArray().toTypedArray()
    val checksum = when (encoding) {
      Encoding.Beck32WithoutChecksum -> arrayOf()
      else -> checksum(hrp, data, encoding)
    }
    return hrp + "1" + (data + checksum).map { i -> alphabet[i.toInt()] }.toCharArray()
      .concatToString()
  }

  /**
   * @param hrp Human Readable Part
   * @param data data (a sequence of 5 bits integers)
   * @param encoding encoding to use (bech32 or bech32m)
   * @return a checksum computed over hrp and data
   */
  private fun checksum(hrp: String, data: Array<Byte>, encoding: Encoding): Array<Byte> {
    val values = expand(hrp) + data
    val poly = polymod(
      values,
      arrayOf(0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
    ) xor encoding.constant
    return Array(6) { i -> (poly.shr(5 * (5 - i)) and 31).toByte() }
  }

  private fun expand(hrp: String): Array<Byte> {
    val result = Array<Byte>(hrp.length + 1 + hrp.length) { 0 }
    for (i in hrp.indices) {
      result[i] = hrp[i].code.shr(5).toByte()
      result[hrp.length + 1 + i] = (hrp[i].code and 31).toByte()
    }
    result[hrp.length] = 0
    return result
  }

  private fun polymod(values: Array<Byte>, values1: Array<Byte>): Int {
    val GEN = arrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)
    var chk = 1
    listOf(values, values1).forEach { vs ->
      vs.forEach { v ->
        val b = chk shr 25
        chk = ((chk and 0x1ffffff) shl 5) xor v.toInt()
        for (i in 0..5) {
          if (((b shr i) and 1) != 0) chk = chk xor GEN[i]
        }
      }
    }
    return chk
  }

  /**
   * @param input a sequence of 8 bits integers
   * @return a sequence of 5 bits integers
   */
  @JvmStatic
  fun eight2five(input: ByteString): Array<Byte> {
    var buffer = 0L
    val output = ArrayList<Byte>()
    var count = 0
    input.toByteArray().forEach { b ->
      buffer = (buffer shl 8) or (b.toLong() and 0xff)
      count += 8
      while (count >= 5) {
        output.add(((buffer shr (count - 5)) and 31).toByte())
        count -= 5
      }
    }
    if (count > 0) output.add(((buffer shl (5 - count)) and 31).toByte())
    return output.toTypedArray()
  }

  /**
   * decodes a bech32 string
   * @param bech32 bech32 string
   * @param noChecksum if true, the bech32 string doesn't have a checksum
   * @return a (hrp, data, encoding) tuple
   */
  @JvmStatic
  fun decodeBytes(
    bech32: String,
    noChecksum: Boolean = false
  ): Triple<String, ByteArray, Encoding> {
    val (hrp, int5s, encoding) = decode(bech32, noChecksum)
    return Triple(hrp, five2eight(int5s, 0), encoding)
  }

  /**
   * decodes a bech32 string
   * @param bech32 bech32 string
   * @param noChecksum if true, the bech32 string doesn't have a checksum
   * @return a (hrp, data, encoding) tuple
   */
  @JvmStatic
  private fun decode(
    bech32: String,
    noChecksum: Boolean = false
  ): Triple<String, Array<Byte>, Encoding> {
    require(bech32.lowercase() == bech32 || bech32.uppercase() == bech32) { "mixed case strings are not valid bech32" }
    bech32.forEach { require(it.code in 33..126) { "invalid character " } }
    val input = bech32.lowercase()
    val pos = input.lastIndexOf('1')
    val hrp = input.take(pos)
    require(hrp.length in 1..83) { "hrp must contain 1 to 83 characters" }
    val data = Array<Byte>(input.length - pos - 1) { 0 }
    for (i in 0..data.lastIndex) data[i] = charToFive[input[pos + 1 + i].code]
    return if (noChecksum) {
      Triple(hrp, data, Encoding.Beck32WithoutChecksum)
    } else {
      val encoding = when (polymod(expand(hrp), data)) {
        Encoding.Bech32.constant -> Encoding.Bech32
        Encoding.Bech32m.constant -> Encoding.Bech32m
        else -> throw IllegalArgumentException("invalid checksum for $bech32")
      }
      Triple(hrp, data.dropLast(6).toTypedArray(), encoding)
    }
  }

  /**
   * @param input a sequence of 5 bits integers
   * @return a sequence of 8 bits integers
   */
  @JvmStatic
  fun five2eight(input: Array<Byte>, offset: Int): ByteArray {
    var buffer = 0L
    val output = ArrayList<Byte>()
    var count = 0
    for (i in offset..input.lastIndex) {
      val b = input[i]
      buffer = (buffer shl 5) or (b.toLong() and 31)
      count += 5
      while (count >= 8) {
        output.add(((buffer shr (count - 8)) and 0xff).toByte())
        count -= 8
      }
    }
    require(count <= 4) { "Zero-padding of more than 4 bits" }
    require((buffer and ((1L shl count) - 1L)) == 0L) { "Non-zero padding in 8-to-5 conversion" }
    return output.toByteArray()
  }

  enum class Encoding(val constant: Int) {
    Bech32(1),
    Bech32m(0x2bc830a3),
    Beck32WithoutChecksum(0)
  }
}
