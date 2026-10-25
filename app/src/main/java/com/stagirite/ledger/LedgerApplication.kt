package com.stagirite.ledger

import android.app.Application
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.security.AppLock
import com.stagirite.ledger.sync.SyncManager
import kotlinx.coroutines.MainScope

class LedgerApplication : Application() {
    lateinit var store: GoalStore
        private set
    lateinit var settings: Settings
        private set
    lateinit var sync: SyncManager
        private set
    lateinit var lock: AppLock
        private set

    override fun onCreate() {
        super.onCreate()
        store = GoalStore(this)
        settings = Settings(this)
        sync = SyncManager(this, store, MainScope())
        lock = AppLock(settings)
        store.onChange = { sync.requestSync(delayMs = 2_000) }
    }
}
