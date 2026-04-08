package com.strangerstrings.habitsync.data.repository

import android.content.Context

class LeaderboardRankStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadTodaySnapshot(): RankSnapshot? {
        val dayKey = preferences.getString(KEY_TODAY_DAY, null) ?: return null
        val ranks = parseRanks(preferences.getString(KEY_TODAY_RANKS, "").orEmpty())
        return RankSnapshot(dayKey = dayKey, ranks = ranks)
    }

    fun loadYesterdaySnapshot(): RankSnapshot? {
        val dayKey = preferences.getString(KEY_YESTERDAY_DAY, null) ?: return null
        val ranks = parseRanks(preferences.getString(KEY_YESTERDAY_RANKS, "").orEmpty())
        return RankSnapshot(dayKey = dayKey, ranks = ranks)
    }

    fun saveTodaySnapshot(snapshot: RankSnapshot) {
        preferences.edit()
            .putString(KEY_TODAY_DAY, snapshot.dayKey)
            .putString(KEY_TODAY_RANKS, encodeRanks(snapshot.ranks))
            .apply()
    }

    fun saveYesterdaySnapshot(snapshot: RankSnapshot) {
        preferences.edit()
            .putString(KEY_YESTERDAY_DAY, snapshot.dayKey)
            .putString(KEY_YESTERDAY_RANKS, encodeRanks(snapshot.ranks))
            .apply()
    }

    private fun encodeRanks(ranks: Map<String, Int>): String {
        return ranks.entries
            .sortedBy { it.key }
            .joinToString(separator = "|") { "${it.key}:${it.value}" }
    }

    private fun parseRanks(encoded: String): Map<String, Int> {
        if (encoded.isBlank()) return emptyMap()
        return encoded.split("|")
            .mapNotNull { token ->
                val parts = token.split(":")
                if (parts.size != 2) return@mapNotNull null
                val userId = parts[0]
                val rank = parts[1].toIntOrNull() ?: return@mapNotNull null
                userId to rank
            }
            .toMap()
    }

    private companion object {
        const val PREFS_NAME = "friends_rank_snapshots"
        const val KEY_TODAY_DAY = "today_day"
        const val KEY_TODAY_RANKS = "today_ranks"
        const val KEY_YESTERDAY_DAY = "yesterday_day"
        const val KEY_YESTERDAY_RANKS = "yesterday_ranks"
    }
}

data class RankSnapshot(
    val dayKey: String,
    val ranks: Map<String, Int>,
)
