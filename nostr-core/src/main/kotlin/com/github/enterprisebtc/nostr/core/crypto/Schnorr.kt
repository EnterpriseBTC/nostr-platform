package com.github.enterprisebtc.nostr.core.crypto

import com.github.enterprisebtc.nostr.core.util.NostrUtil
import java.math.BigInteger
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SecureRandom

/**
 * Schnorr signature implementation for Secp256k1.
 */
object Schnorr {

    private const val RANDOM_NUMBER_ALGORITHM = "SHA1PRNG"
    private const val RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN"

    private val MAXPRIVATEKEY = BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364140", 16)

    /**
     * Signs a message using the Schnorr signature scheme.
     *
     * @param msg The message to sign.
     * @param secKey The secret key used for signing.
     * @param auxRand Auxiliary random data.
     * @return The signature.
     * @throws Exception If an error occurs during signing.
     */
    @Throws(Exception::class)
    fun sign(msg: ByteArray, secKey: ByteArray, auxRand: ByteArray): ByteArray {
        require(msg.size == 32) { "The message must be a 32-byte array." }
        var secKey0 = NostrUtil.bigIntFromBytes(secKey)

        require(BigInteger.ONE <= secKey0 && secKey0 <= Point.n - BigInteger.ONE) {
            "The secret key must be an integer in the range 1..n-1."
        }
        val P = Point.mul(Point.G, secKey0) ?: throw Exception("Point multiplication failed.")
        if (!P.hasEvenY()) {
            secKey0 = Point.n - secKey0
        }
        val len = NostrUtil.bytesFromBigInteger(secKey0).size + P.toBytes().size + msg.size
        val buf = ByteArray(len)
        val t = NostrUtil.xor(NostrUtil.bytesFromBigInteger(secKey0), Point.taggedHash("BIP0340/aux", auxRand))!!
        System.arraycopy(t, 0, buf, 0, t.size)
        System.arraycopy(P.toBytes(), 0, buf, t.size, P.toBytes().size)
        System.arraycopy(msg, 0, buf, t.size + P.toBytes().size, msg.size)
        var k0 = NostrUtil.bigIntFromBytes(Point.taggedHash("BIP0340/nonce", buf)).mod(Point.n)
        if (k0 == BigInteger.ZERO) {
            throw Exception("Failure. This happens only with negligible probability.")
        }
        val R = Point.mul(Point.G, k0) ?: throw Exception("Point multiplication failed.")
        val k = if (!R.hasEvenY()) Point.n - k0 else k0
        val sigLen = R.toBytes().size + NostrUtil.bytesFromBigInteger(k).size
        val sig = ByteArray(sigLen)
        System.arraycopy(R.toBytes(), 0, sig, 0, R.toBytes().size)
        System.arraycopy(NostrUtil.bytesFromBigInteger(k), 0, sig, R.toBytes().size, NostrUtil.bytesFromBigInteger(k).size)
        if (!verify(msg, P.toBytes(), sig)) {
            throw Exception("The signature does not pass verification.")
        }
        return sig
    }

    /**
     * Verifies a Schnorr signature.
     *
     * @param msg The signed message.
     * @param pubkey The public key used for verification.
     * @param sig The signature to verify.
     * @return True if the signature is valid, false otherwise.
     * @throws Exception If an error occurs during verification.
     */
    @Throws(Exception::class)
    fun verify(msg: ByteArray, pubkey: ByteArray, sig: ByteArray): Boolean {
        require(msg.size == 32) { "The message must be a 32-byte array." }
        require(pubkey.size == 32) { "The public key must be a 32-byte array." }
        require(sig.size == 64) { "The signature must be a 64-byte array." }

        val P = Point.liftX(pubkey) ?: return false
        val r = NostrUtil.bigIntFromBytes(sig.copyOfRange(0, 32))
        val s = NostrUtil.bigIntFromBytes(sig.copyOfRange(32, 64))
        if (r >= Point.p || s >= Point.n) {
            return false
        }
        val len = 32 + pubkey.size + msg.size
        val buf = ByteArray(len)
        System.arraycopy(sig, 0, buf, 0, 32)
        System.arraycopy(pubkey, 0, buf, 32, pubkey.size)
        System.arraycopy(msg, 0, buf, 32 + pubkey.size, msg.size)
        val e = NostrUtil.bigIntFromBytes(Point.taggedHash("BIP0340/challenge", buf)).mod(Point.n)
        val R = Point.add(Point.mul(Point.G, s), Point.mul(P, Point.n - e))
        return R != null && R.hasEvenY() && R.getX() == r
    }

    /**
     * Generates a random private key that can be used with Secp256k1.
     *
     * @return The generated private key.
     */
    fun generatePrivateKey(): ByteArray {
        val secureRandom = try {
            SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER)
        } catch (e: NoSuchAlgorithmException) {
            SecureRandom()
        } catch (e: NoSuchProviderException) {
            SecureRandom()
        }

        // Generate the key, skipping as many as desired.
        val privateKeyAttempt = ByteArray(32)
        var privateKeyCheck: BigInteger
        do {
            secureRandom.nextBytes(privateKeyAttempt)
            privateKeyCheck = BigInteger(1, privateKeyAttempt)
        } while (privateKeyCheck == BigInteger.ZERO || privateKeyCheck > MAXPRIVATEKEY)

        return privateKeyAttempt
    }

    /**
     * Generates a public key from a given private key.
     *
     * @param secKey The private key.
     * @return The corresponding public key.
     * @throws Exception If the secret key is invalid.
     */
    @Throws(Exception::class)
    fun genPubKey(secKey: ByteArray): ByteArray {
        val x = NostrUtil.bigIntFromBytes(secKey)
        require(BigInteger.ONE <= x && x <= Point.n - BigInteger.ONE) {
            "The secret key must be an integer in the range 1..n-1."
        }
        val ret = Point.mul(Point.G, x) ?: throw Exception("Point multiplication failed.")
        return Point.bytesFromPoint(ret)
    }
}