package com.wordquest.app.data

/**
 * Deterministic RNG (SplitMix64) so the same level index always produces the
 * same puzzle/question set across app launches and devices — ported from
 * iOS's SeededGenerator, which is the same algorithm.
 *
 * Note: this reproduces the *seed formula and generation approach* from the
 * iOS app (same theme+level seeding, same word-placement algorithm), not a
 * bit-for-bit identical output stream. Swift's `shuffled(using:)` /
 * `random(in:using:)` use their own internal bounded-random implementation on
 * top of the raw generator; [nextLong] here uses a standard unbiased
 * rejection-sampling scheme instead. The two apps are separate binaries with
 * no shared save data, so this is not user-observable — what matters is that
 * Android's own (theme, level) pair is always deterministic and distinct.
 */
class SeededGenerator(seed: Long) {
    private var state: Long = seed + -0x61c8864680b583ebL // 0x9E3779B97F4A7C15 as signed Long

    fun nextULong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** Unbiased random Long in [0, bound) via rejection sampling. */
    fun nextLong(bound: Long): Long {
        require(bound > 0)
        val limit = Long.MAX_VALUE - Long.MAX_VALUE % bound
        while (true) {
            val bits = nextULong() ushr 1 // clear sign bit -> non-negative
            if (bits < limit) return bits % bound
        }
    }

    fun nextInt(bound: Int): Int = nextLong(bound.toLong()).toInt()

    fun <T> pick(list: List<T>): T? = if (list.isEmpty()) null else list[nextInt(list.size)]

    fun <T> shuffled(list: List<T>): List<T> {
        val result = list.toMutableList()
        for (i in result.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val tmp = result[i]
            result[i] = result[j]
            result[j] = tmp
        }
        return result
    }
}
