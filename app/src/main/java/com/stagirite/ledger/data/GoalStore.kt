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

    fun add(title: String, since: LocalDate, reason: String = "", icon: String = "", kind: GoalKind? = null, sealed: String? = null) {
        val now = System.currentTimeMillis()
        val goal = Goal(UUID.randomUUID().toString(), "", since, createdAt = now, updatedAt = now)
            .edit(title, since, reason, now, icon, kind, sealed)
        all.add(goal)
        changed()
    }

    fun edit(id: String, title: String, since: LocalDate, reason: String, icon: String, kind: GoalKind?, sealed: String?) =
        replace(id) { it.edit(title, since, reason, System.currentTimeMillis(), icon, kind, sealed) }

    /** Applies [change] to every goal it alters (returns non-null for). */
    fun updateAll(change: (Goal) -> Goal?) {
        var any = false
        all.indices.forEach { i ->
            change(all[i])?.let { all[i] = it.copy(updatedAt = System.currentTimeMillis()); any = true }
        }
        if (any) changed()
    }

    fun moveCurrentStart(id: String, day: LocalDate) =
        replace(id) { it.moveCurrentStart(day, System.currentTimeMillis()) }

    fun recordSlip(id: String, day: LocalDate) =
        replace(id) { it.recordSlip(day, System.currentTimeMillis()) }

    fun removeSlip(id: String, day: LocalDate) =
        replace(id) { it.removeSlip(day, System.currentTimeMillis()) }

    /** Deletes a goal and returns it, so the deletion can be undone with [restore]. */
    fun delete(id: String): Goal? {
        val goal = find(id) ?: return null
        replace(id) { it.tombstone(System.currentTimeMillis()) }
        return goal
    }

    fun restore(goal: Goal) {
        val i = all.indexOfFirst { it.id == goal.id }
        val revived = goal.copy(deleted = false, updatedAt = System.currentTimeMillis())
        if (i >= 0) all[i] = revived else all.add(revived)
        changed()
    }

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
