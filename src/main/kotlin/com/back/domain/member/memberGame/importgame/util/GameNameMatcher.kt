package com.back.domain.member.memberGame.importgame.util

import java.util.regex.Pattern

object GameNameMatcher {
    private val TRADEMARK_SYMBOLS = Pattern.compile("[\u00AE\u2122\u00A9]")
    private val EDITION_SUFFIX = Pattern.compile(
        """\s*[-–:]\s*(ultimate|deluxe|gold|goty|game of the year|standard|premium|""" +
            """digital|complete|definitive|enhanced|remastered|legendary|collector'?s|""" +
            """special|limited|anniversary|expanded)\s*(edition|ver\.?|version)?\s*$""",
        Pattern.CASE_INSENSITIVE,
    )
    private val NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]")
    private val EXTRA_SPACES = Pattern.compile("\\s+")

    fun normalize(name: String?): String {
        if (name == null) return ""
        var s = name.lowercase().trim()
        s = TRADEMARK_SYMBOLS.matcher(s).replaceAll("")
        s = EDITION_SUFFIX.matcher(s).replaceAll("")
        s = NON_ALPHANUMERIC.matcher(s).replaceAll(" ")
        s = EXTRA_SPACES.matcher(s).replaceAll(" ")
        return s.trim()
    }

    fun computeConfidence(
        name1: String?,
        name2: String?,
    ): Double {
        val n1 = normalize(name1)
        val n2 = normalize(name2)
        if (n1.isEmpty() || n2.isEmpty()) return 0.0
        if (n1 == n2) return 1.0
        val distance = levenshteinDistance(n1, n2)
        val maxLen = maxOf(n1.length, n2.length)
        return 1.0 - (distance.toDouble() / maxLen)
    }

    private fun levenshteinDistance(
        a: String,
        b: String,
    ): Int {
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[b.length]
    }
}