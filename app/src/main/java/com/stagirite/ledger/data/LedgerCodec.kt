package com.stagirite.ledger.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** The ledger's JSON format, used for local storage, sync and export alike. */
object LedgerCodec {
    private const val VERSION = 2

    fun encode(goals: List<Goal>): String {
        val array = JSONArray()
        goals.forEach { g ->
            val slips = JSONArray()
            g.slips.forEach { slips.put(it.toEpochDay()) }
            array.put(
                JSONObject()
                    .put("id", g.id)
                    .put("title", g.title)
                    .put("since", g.since.toEpochDay())
                    .put("slips", slips)
                    .put("reason", g.reason)
                    .put("icon", g.icon)
                    .put("kind", g.kind?.name ?: "")
                    .put("sealed", g.sealed ?: "")
                    .put("created", g.createdAt)
                    .put("updated", g.updatedAt)
                    .put("deleted", g.deleted)
            )
        }
        return JSONObject().put("version", VERSION).put("goals", array).toString()
    }

    /** Reads version 1 or 2. Throws [org.json.JSONException] if [text] isn't a ledger. */
    fun decode(text: String): List<Goal> {
        val array = JSONObject(text).getJSONArray("goals")
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val (since, slips) = if (o.has("since")) {
                val s = o.optJSONArray("slips") ?: JSONArray()
                LocalDate.ofEpochDay(o.getLong("since")) to (0 until s.length()).map { LocalDate.ofEpochDay(s.getLong(it)) }
            } else {
                // Version 1: the current run's start plus [start, end] pairs of past runs.
                val h = o.optJSONArray("history") ?: JSONArray()
                val runs = (0 until h.length()).map { h.getJSONArray(it) }
                val first = runs.firstOrNull()?.getLong(0) ?: o.getLong("start")
                LocalDate.ofEpochDay(first) to runs.map { LocalDate.ofEpochDay(it.getLong(1)) }
            }
            Goal(
                id = o.getString("id"),
                title = o.optString("title"),
                since = since,
                slips = slips.filter { !it.isBefore(since) }.distinct().sorted(),
                reason = o.optString("reason"),
                icon = o.optString("icon"),
                kind = GoalKind.entries.firstOrNull { it.name == o.optString("kind") },
                sealed = o.optString("sealed").ifEmpty { null },
                createdAt = o.optLong("created"),
                updatedAt = o.optLong("updated"),
                deleted = o.optBoolean("deleted"),
            )
        }
    }
}
