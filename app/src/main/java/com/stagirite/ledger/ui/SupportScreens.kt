package com.stagirite.ledger.ui

import com.stagirite.ledger.BuildConfig
import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stagirite.ledger.security.AppLock
import com.stagirite.ledger.support.Billing
import com.stagirite.ledger.support.Products
import com.stagirite.ledger.support.SupportAd
import kotlinx.coroutines.launch

/** Whether Plus is unlocked, and a way to offer it with a reason ("Unlimited goals …"). */
class PlusUi(val has: Boolean, val offer: (reason: String) -> Unit)

val LocalPlus = staticCompositionLocalOf { PlusUi(has = true, offer = {}) }

/** What the Support section and the Plus sheet need. */
class SupportUi(val billing: Billing, val ad: SupportAd, val lock: AppLock)

val LocalSupport = staticCompositionLocalOf<SupportUi?> { null }

private val included = listOf(
    "Unlimited goals (free includes ${Billing.FREE_GOALS})",
    "Sync with your Google Drive, encrypted if you like",
    "The Goals widget, with several goals at once",
    "Insights: the Progress card, year view and weekly review",
)

private val alwaysFree = listOf(
    "Counting days, slips, notes, urges and milestones",
    "Calendar, stats and history",
    "App lock and private titles",
    "Automatic backup and export",
    "The Goal and Overview widgets",
)

/** The Plus offer: what it includes, what stays free, one price. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusSheet(reason: String, support: SupportUi, onDismiss: () -> Unit) {
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val billing = support.billing
    val price = billing.prices[Products.PLUS]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text("Stagira Plus", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            SectionHeading("Plus adds", Modifier.padding(start = 0.dp))
            included.forEach { Tick(it) }
            Spacer(Modifier.height(16.dp))
            SectionHeading("Always free", Modifier.padding(start = 0.dp))
            alwaysFree.forEach { Tick(it, muted = true) }
            Spacer(Modifier.height(24.dp))
            if (billing.plus) {
                Text("You have Plus. Thank you.", style = MaterialTheme.typography.titleMedium)
            } else if (!billing.available || price == null) {
                Text(
                    "Purchases work in the version from Google Play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = {
                        support.lock.expectReturn()
                        billing.buy(activity, Products.PLUS)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Get Plus · $price") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "One payment, yours for good. No subscription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                TextButton(
                    onClick = { scope.launch { billing.restore() } },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Restore purchase") }
            }
        }
    }
}

@Composable
private fun Tick(text: String, muted: Boolean = false) {
    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (muted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Settings → Support Stagira: Plus, the tip jar, and an optional ad. */
@Composable
fun SupportSection(support: SupportUi) {
    val context = LocalContext.current
    val activity = context as Activity
    val billing = support.billing
    val plus = LocalPlus.current

    // Plus
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Stagira Plus", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (BuildConfig.PLUS_FOR_TESTING) "Unlocked for testing." else if (billing.plus) "Unlocked. Thank you." else "Unlimited goals, sync, the Goals widget and insights. One payment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!billing.plus) TextButton(onClick = { plus.offer("Support the ledger and unlock everything.") }) { Text("See Plus") }
    }

    // Tip jar
    Spacer(Modifier.height(16.dp))
    Text("Tip jar", style = MaterialTheme.typography.bodyLarge)
    Text(
        "A one-off thank-you. It changes nothing in the app, except the developer’s day.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    if (billing.available && Products.tips.all { it in billing.prices }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Products.tips.forEach { id ->
                OutlinedButton(onClick = {
                    support.lock.expectReturn()
                    billing.buy(activity, id)
                }) { Text(billing.prices.getValue(id)) }
            }
        }
    } else {
        Text(
            "Tips work in the version from Google Play.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    // Rewarded ad
    Spacer(Modifier.height(16.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Watch an ad to support us", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Free for you; it pays the developer a little. Ads only ever appear if you tap this. " +
                    "It uses Google’s ad service, which may collect device information; ads about " +
                    "alcohol, gambling and dating are blocked.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (support.ad.state == SupportAd.State.Idle) {
            TextButton(onClick = {
                support.lock.expectReturn()
                support.ad.watch(activity) { watched, problem ->
                    val message = when {
                        watched -> "Thank you. That helps."
                        problem != null -> problem
                        else -> null
                    }
                    message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                }
            }) { Text("Watch") }
        } else {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
