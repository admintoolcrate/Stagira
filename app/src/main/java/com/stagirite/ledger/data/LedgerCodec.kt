package com.stagirite.ledger.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** The ledger's JSON format, used for local storage, sync and export alike. */
object LedgerCodec {
    private const val VERSION = 1

    fun encode(goals: List<Goal>): String {
        val array = JSONArray()
        goals.forEach { g ->
            val history = JSONArray()
            g.history.forEach { s ->
                history.put(JSONArray().put(s.start.toEpochDay()).put(s.end.toEpochDay()))
            }
            array.put(
                JSONObject()
                    .put("id", g.id)
                    .put("title", g.title)
                    .put("start", g.startDate.toEpochDay())
                    .put("history", history)
                    .put("created", g.createdAt)
                    .put("updated", g.updatedAt)
                    .put("deleted", g.deleted)
            )
        }
        return JSONObject().put("version", VERSION).put("goals", array).toString()
    }

    /** Throws [org.json.JSONException] if [text] isn't a ledger. */
    fun decode(text: String): List<Goal> {
        val array = JSONObject(text).getJSONArray("goals")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val h = o.optJSONArray("history") ?: JSONArray()
            Goal(
                id = o.getString("id"),
                title = o.optString("title"),
                startDate = LocalDate.ofEpochDay(o.getLong("start")),
                history = (0 until h.length()).map { j ->
                    val pair = h.getJSONArray(j)
                    Streak(LocalDate.ofEpochDay(pair.getLong(0)), LocalDate.ofEpochDay(pair.getLong(1)))
                },
                createdAt = o.optLong("created"),
                updatedAt = o.optLong("updated"),
                deleted = o.optBoolean("deleted"),
            )
        }
    }
}
