package fr.troupel.whereami.util

import java.text.Normalizer
import kotlin.math.min

fun String.stripAccents(): String {
    val normalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    return normalized.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
}

fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
    if (lhs == rhs) {
        return 0
    }
    if (lhs.isEmpty()) {
        return rhs.length
    }
    if (rhs.isEmpty()) {
        return lhs.length
    }

    val lhsLength = lhs.length + 1
    val rhsLength = rhs.length + 1

    var cost = Array(lhsLength) { it }
    var newCost = Array(lhsLength) { 0 }

    for (i in 1..<rhsLength) {
        newCost[0] = i

        for (j in 1..<lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = min(min(costInsert, costDelete), costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength - 1]
}

fun jaro(s1: String, s2: String): Double {
    if (s1 == s2) return 1.0

    val len1 = s1.length
    val len2 = s2.length
    if (len1 == 0 || len2 == 0) return .0

    // maximum distance two chars can be apart to be considered matching
    val matchDist = (maxOf(len1, len2) / 2) - 1

    val s1Matches = BooleanArray(len1)
    val s2Matches = BooleanArray(len2)
    var matches = 0
    for (i in 0 until len1) {
        val start = maxOf(0, i - matchDist)
        val end = minOf(len2 - 1, i + matchDist)
        for (j in start..end) {
            if (!s2Matches[j] && s1[i] == s2[j]) {
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
    }
    if (matches == 0) return .0

    // count transpositions (half the number of matched chars that differ in order)
    var t = 0
    var k = 0
    for (i in 0 until len1) {
        if (s1Matches[i]) {
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) t++
            k++
        }
    }
    val transpositions = t / 2.0

    // Jaro similarity formula
    return (matches / len1.toDouble()
            + matches / len2.toDouble()
            + (matches - transpositions) / matches.toDouble()
            ) / 3.0
}

/**
 * Computes the Jaro-Winkler similarity between two strings.
 * @param prefixScale scaling factor for how much the common prefix boosts the score (standard is 0.1)
 * @param maxPrefixLength the maximum prefix length to use (standard is 4)
 */
fun jaroWinkler(
    s1: String, s2: String,
    prefixScale: Double = .1,
    maxPrefixLength: Int = 4
): Double {
    val jaroDist = jaro(s1, s2)

    // find length of common prefix up to maxPrefixLength
    var prefixLength = 0
    val n = minOf(s1.length, s2.length, maxPrefixLength)
    for (i in 0 until n) {
        if (s1[i] == s2[i]) prefixLength++ else break
    }

    return jaroDist + prefixLength * prefixScale * (1 - jaroDist)
}