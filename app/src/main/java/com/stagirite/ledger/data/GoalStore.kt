package com.stagirite.ledger.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.time.LocalDate
import java.util.UUID

/**
 * The ledger on this device, saved in app-private storage. Holds deleted goals
 * as tombstones too, so deletions can be synced; [goals] hides them.
 */
class GoalStore(context: Context) {
    private val prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE)
    private val all = mutableStateListOf<Goal>().apply { addAll(load()) }

    /** Called after every local change, so sync can push it. */
    var onChange: () -> Unit = {}

    val goals: List<Goal> get() = all.filter { !it.deleted }

    /** Everything including tombstones, for sync and export. */
    val snapshot: List<Goal> get() = all.toList()

    fun find(id: String): Goal? = all.firstOrNull { it.id == id && !it.deleted }

    fun add(title: String, startDate: LocalDate) {
        val now = System.currentTimeMillis()
        all.add(Goal(UUID.randomUUID().toString(), title.trim(), startDate, createdAt = now, updatedAt = now))
        changed()
    }

    fun edit(id: String, title: String, startDate: LocalDate) =
        replace(id) { it.edit(title, startDate, System.currentTimeMillis()) }

    fun reset(id: String, today: LocalDate) =
        replace(id) { it.reset(today, System.currentTimeMillis()) }

    fun delete(id: String) =
        replace(id) { it.tombstone(System.currentTimeMillis()) }

    /** Folds in goals from elsewhere (sync or import). Returns true if anything changed. */
    fun mergeIn(other: List<Goal>): Boolean {
        val merged = mergeGoals(all, other)
        if (merged == all.toList()) return false
        all.clear()
        all.addAll(merged)
        save()
        return true
    }

    /** Removes every goal from this device. */
    fun clear() {
        all.clear()
        save()
    }

    private fun replace(id: String, change: (Goal) -> Goal) {
        val i = all.indexOfFirst { it.id == id }
        if (i < 0) return
        all[i] = change(all[i])
        changed()
    }

    private fun changed() {
        save()
        onChange()
    }

    private fun load(): List<Goal> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { LedgerCodec.decode(raw) }.getOrDefault(emptyList())
    }

    private fun save() {
        prefs.edit().putString(KEY, LedgerCodec.encode(all)).apply()
    }

    private companion object {
        const val KEY = "ledger"
    }
}
