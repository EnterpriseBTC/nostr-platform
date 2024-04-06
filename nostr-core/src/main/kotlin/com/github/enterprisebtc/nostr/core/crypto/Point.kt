package com.github.enterprisebtc.nostr.core.crypto

import com.github.enterprisebtc.nostr.core.util.NostrUtil
import java.math.BigInteger
import java.security.NoSuchAlgorithmException

data class Point private constructor(private val pair: Pair<BigInteger?, BigInteger?>) {

    constructor(x: BigInteger?, y: BigInteger?) : this(Pair(x, y))
    constructor(b0: ByteArray, b1: ByteArray) : this(Pair(BigInteger(1, b0), BigInteger(1, b1)))

    fun getX(): BigInteger? = pair.first
    fun getY(): BigInteger? = pair.second
    fun isInfinite(): Boolean = pair.first == null || pair.second == null
    fun add(other: Point): Point = add(this, other)
    fun toBytes(): ByteArray = bytesFromPoint(this)
    fun hasEvenY(): Boolean = hasEvenY(this)
    fun hasSquareY(): Boolean = hasSquareY(this)

    companion object {
        val p = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)
        val n = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)

        val G = Point(
            BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16),
            BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
        )

        private val BI_TWO = BigInteger.valueOf(2)

        val infinityPoint: Point = Point(null, null)

        fun add(P1: Point?, P2: Point?): Point {
            if (P1 != null && P2 != null && P1.isInfinite() && P2.isInfinite()) {
                return infinityPoint
            }
            if (P1 == null || P1.isInfinite()) {
                return P2 ?: infinityPoint
            }
            if (P2 == null || P2.isInfinite()) {
                return P1
            }
            if (P1.getX() == P2.getX() && P1.getY() != P2.getY()) {
                return infinityPoint
            }

            val lam = if (P1 == P2) {
                val base = P2.getY()!! * BI_TWO
                (BigInteger.valueOf(3) * P1.getX()!! * P1.getX()!! * base.modPow(p - BI_TWO, p)).mod(p)
            } else {
                val base = P2.getX()!! - P1.getX()!!
                ((P2.getY()!! - P1.getY()!!) * base.modPow(p - BI_TWO, p)).mod(p)
            }

            val x3 = (lam * lam - P1.getX()!! - P2.getX()!!).mod(p)
            return Point(x3, lam * (P1.getX()!! - x3) - P1.getY()!!.mod(p))
        }

        fun subtract(p: Point, other: Point): Point = add(p, negate(other))

        fun negate(p: Point): Point = Point(p.getX(), mod(-p.getY()!!))

        fun mod(a: BigInteger): BigInteger = a.mod(p)

        fun mul(P: Point, n: BigInteger): Point? {
            var R: Point? = null
            var mP = P

            for (i in 0 until 256) {
                if (n.shiftRight(i).and(BigInteger.ONE) > BigInteger.ZERO) {
                    R = add(R, mP)
                }
                mP = add(mP, mP)
            }

            return R
        }

        fun isSquare(x: BigInteger): Boolean = x.modPow(p - BigInteger.ONE / BI_TWO, p).toLong() == 1L

        @Throws(NoSuchAlgorithmException::class)
        fun taggedHash(tag: String, msg: ByteArray): ByteArray {
            val tagHash = NostrUtil.sha256(tag.toByteArray())
            val len = tagHash.size * 2 + msg.size
            val buf = ByteArray(len)
            System.arraycopy(tagHash, 0, buf, 0, tagHash.size)
            System.arraycopy(tagHash, 0, buf, tagHash.size, tagHash.size)
            System.arraycopy(msg, 0, buf, tagHash.size * 2, msg.size)

            return NostrUtil.sha256(buf)
        }

        fun bytesFromPoint(P: Point): ByteArray = NostrUtil.bytesFromBigInteger(P.getX()!!)

        fun liftX(b: ByteArray): Point? {
            val x = NostrUtil.bigIntFromBytes(b)
            if (x >= p) {
                return null
            }
            val y_sq = x.modPow(BigInteger.valueOf(3), p) + BigInteger.valueOf(7) % p
            val y = y_sq.modPow(p + BigInteger.ONE / BigInteger.valueOf(4), p)

            return if (y.modPow(BI_TWO, p) != y_sq) {
                null
            } else {
                Point(x, if (y.and(BigInteger.ONE) == BigInteger.ZERO) y else p - y)
            }
        }

        fun isInfinite(P: Point): Boolean = P == infinityPoint

        fun getX(P: Point): BigInteger? {
            assert(!P.isInfinite())
            return P.getX()
        }

        fun getY(P: Point): BigInteger? {
            assert(!P.isInfinite())
            return P.getY()
        }

        fun hasEvenY(P: Point): Boolean = P.getY()!! % BI_TWO == BigInteger.ZERO

        fun hasSquareY(P: Point): Boolean {
            assert(!isInfinite(P))
            return isSquare(P.getY()!!)
        }
    }
}