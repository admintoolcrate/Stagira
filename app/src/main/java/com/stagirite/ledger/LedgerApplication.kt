package com.stagirite.ledger

import com.stagirite.ledger.feedback.CrashLog
import com.stagirite.ledger.support.PlayServices
import com.stagirite.ledger.beta.BetaUpdates
import android.app.Application
import com.stagirite.ledger.data.GoalStore
import com.stagirite.ledger.data.Settings
import com.stagirite.ledger.security.AppLock
import com.stagirite.ledger.security.PrivateTitles
import com.stagirite.ledger.support.Billing
import com.stagirite.ledger.support.SupportAd
import com.stagirite.ledger.sync.SyncManager
import com.stagirite.ledger.widget.LedgerWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LedgerApplication : Application() {
    lateinit var store: GoalStore
        private set
    lateinit var settings: Settings
        private set
    lateinit var sync: SyncManager
        private set
    lateinit var lock: AppLock
        private set
    lateinit var privateTitles: PrivateTitles
        private set
    lateinit var billing: Billing
        private set
    val supportAd = SupportAd()
    lateinit var beta: BetaUpdates
    lateinit var play: PlayServices

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        store = GoalStore(this)
        settings = Settings(this)
        sync = SyncManager(this, store, MainScope())
        lock = AppLock(settings)
        privateTitles = PrivateTitles(this)
        val scope = MainScope()
        billing = Billing(this, scope)
        beta = BetaUpdates(this)
        play = PlayServices(this)
        store.onChange = {
            sync.requestSync(delayMs = 2_000)
            scope.launch { runCatching { LedgerWidget.refresh(this@LedgerApplication) } }
        }
    }
}
