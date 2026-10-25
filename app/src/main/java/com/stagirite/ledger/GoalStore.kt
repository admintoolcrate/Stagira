package com.stagirite.ledger

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** Keeps the goals in SharedPreferences as a small JSON array. */
class GoalStore(context: Context) {
    private val prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE)

    val goals = mutableStateListOf<Goal>().apply { addAll(load()) }

    fun add(title: String, startDate: LocalDate) {
        goals.add(Goal(id = UUID.randomUUID().toString(), title = title.trim(), startDate = startDate))
        save()
    }

    fun update(goal: Goal) {
        val i = goals.indexOfFirst { it.id == goal.id }
        if (i >= 0) {
            goals[i] = goal
            save()
        }
    }

    fun delete(id: String) {
        goals.removeAll { it.id == id }
        save()
    }

    private fun load(): List<Goal> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Goal(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    startDate = LocalDate.ofEpochDay(o.getLong("start")),
                    bestStreak = o.optLong("best", 0),
                    resets = o.optInt("resets", 0),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save() {
        val array = JSONArray()
        goals.forEach { g ->
            array.put(
                JSONObject()
                    .put("id", g.id)
                    .put("title", g.title)
                    .put("start", g.startDate.toEpochDay())
                    .put("best", g.bestStreak)
                    .put("resets", g.resets)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "goals"
    }
}
