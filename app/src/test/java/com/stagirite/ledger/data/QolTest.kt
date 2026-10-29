package com.stagirite.ledger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class QolTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val today = start.plusDays(45)

    @Test
    fun targetProgress() {
        val g = Goal("g", "T", start).edit("T", start, "", now = 1, target = 90)
        assertEquals(90L, g.target)
        assertEquals(0.5f, g.targetProgress(today)!!, 1e-6f)
        assertEquals(1f, g.targetProgress(start.plusDays(200))!!, 0f)
        assertNull(Goal("g", "T", start).targetProgress(today))
        // A target of 0 means none.
        assertNull(g.edit("T", start, "", now = 2, target = 0).target)
    }

    @Test
    fun codecKeepsTargetAndArchived() {
        val g = Goal("g", "T", start, createdAt = 1).copy(target = 30, archived = true)
        assertEquals(listOf(g), LedgerCodec.decode(LedgerCodec.encode(listOf(g))))
        val plain = Goal("h", "U", start, createdAt = 2)
        val back = LedgerCodec.decode(LedgerCodec.encode(listOf(plain))).single()
        assertNull(back.target)
        assertEquals(false, back.archived)
    }

    @Test
    fun tombstoneClearsTargetAndArchive() {
        val t = Goal("g", "T", start).copy(target = 30, archived = true).tombstone(5)
        assertNull(t.target)
        assertEquals(false, t.archived)
    }

    @Test
    fun sorting() {
        val a = Goal("a", "banana", today.minusDays(10)) // 10 days, next 14 → 4 away
        val b = Goal("b", "Apple", today.minusDays(40)) // 40 days, next 60 → 20 away
        val c = Goal("c", "cherry", today.minusDays(2)) // 2 days, next 3 → 1 away
        val goals = listOf(a, b, c)
        assertEquals(listOf("a", "b", "c"), sortGoals(goals, SortOrder.Added, today).map { it.id })
        assertEquals(listOf("b", "a", "c"), sortGoals(goals, SortOrder.LongestRun, today).map { it.id })
        assertEquals(listOf("c", "a", "b"), sortGoals(goals, SortOrder.NextMilestone, today).map { it.id })
        assertEquals(listOf("b", "a", "c"), sortGoals(goals, SortOrder.Name, today).map { it.id })
    }

    @Test
    fun templatesUseIconsFromTheSet() {
        goalTemplates.forEach { assert(it.icon in GoalIcons.all) { it.icon } }
    }
}
