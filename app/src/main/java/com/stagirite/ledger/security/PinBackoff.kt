package com.stagirite.ledger.security

/**
 * How long to wait after [failures] wrong PINs in a row: nothing for the first
 * [free], then 30 s, doubling with each further miss, capped at an hour.
 */
fun pinBackoffMillis(failures: Int, free: Int = 5): Long {
    val extra = failures - free
    if (extra <= 0) return 0
    return minOf(30_000L shl minOf(extra - 1, 7), 3_600_000L)
}
