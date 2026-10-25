package com.stagirite.ledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stagirite.ledger.ui.LedgerApp
import com.stagirite.ledger.ui.LedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = GoalStore(applicationContext)
        setContent {
            LedgerTheme {
                LedgerApp(store)
            }
        }
    }
}
