package com.stagirite.ledger.support

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.stagirite.ledger.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Product ids. Create these as one-time products in Play Console → Monetize → In-app products. */
object Products {
    const val PLUS = "plus"
    const val TIP_SMALL = "tip_small"
    const val TIP_MEDIUM = "tip_medium"
    const val TIP_LARGE = "tip_large"
    val tips = listOf(TIP_SMALL, TIP_MEDIUM, TIP_LARGE)
}

/**
 * Plus (a one-time unlock) and the tip jar, through Google Play Billing.
 * There's no server: Plus is remembered on the device and confirmed against
 * Google Play whenever it can be reached. Test builds have Plus unlocked.
 */
class Billing(context: Context, private val scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("support", Context.MODE_PRIVATE)
    private val details = mutableMapOf<String, ProductDetails>()

    var plus by mutableStateOf(BuildConfig.PLUS_FOR_TESTING || prefs.getBoolean("plus", false))
        private set

    /** Local prices, e.g. "£4.99", once Google Play has answered. */
    var prices by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    /** False when Google Play isn't available (e.g. an APK installed from GitHub). */
    var available by mutableStateOf(false)
        private set

    /** Set after a purchase or tip goes through; the UI shows it once and clears it. */
    var thanks by mutableStateOf<String?>(null)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> if (result.ok) purchases?.forEach(::handle) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        connect()
    }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (!result.ok) return
                available = true
                scope.launch {
                    loadProducts()
                    restore()
                }
            }

            override fun onBillingServiceDisconnected() {
                available = false
            }
        })
    }

    private suspend fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            (listOf(Products.PLUS) + Products.tips).map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
        ).build()
        // Billing 8 reports found and unfound products separately; only found ones have prices.
        val found = suspendCancellableCoroutine<List<ProductDetails>> { cont ->
            client.queryProductDetailsAsync(params) { result, products ->
                cont.resume(if (result.ok) products.productDetailsList else emptyList())
            }
        }
        found.forEach { details[it.productId] = it }
        prices = details.mapValues { it.value.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty() }
    }

    /** Re-checks purchases in the background (reconnecting if needed), e.g. when the app opens. */
    fun refresh() {
        if (client.isReady) scope.launch { restore() } else connect()
    }

    /** Checks Google Play for Plus, e.g. on a new phone or after reinstalling. */
    suspend fun restore() {
        if (!client.isReady) return
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        )
        if (!result.billingResult.ok) return
        val owned = result.purchasesList.any { Products.PLUS in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        rememberPlus(owned)
        result.purchasesList.forEach(::handle)
    }

    /** Opens Google Play's purchase sheet. False if the product isn't available. */
    fun buy(activity: Activity, productId: String): Boolean {
        val product = details[productId] ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
        ).build()
        return client.launchBillingFlow(activity, params).ok
    }

    private fun handle(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        scope.launch {
            if (Products.PLUS in purchase.products) {
                rememberPlus(true)
                if (!purchase.isAcknowledged) {
                    client.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                    )
                    thanks = "Thank you. Plus is yours, for good."
                }
            } else if (purchase.products.any { it in Products.tips }) {
                // Tips are consumed so they can be given again.
                val consumed = client.consumePurchase(
                    ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                )
                if (consumed.billingResult.ok) thanks = "Thank you for the tip. It genuinely helps."
            }
        }
    }

    /** Remembers what Google Play says; a refund turns Plus off again at the next check. */
    private fun rememberPlus(owned: Boolean) {
        prefs.edit().putBoolean("plus", owned).apply()
        plus = owned || BuildConfig.PLUS_FOR_TESTING
    }

    private val BillingResult.ok get() = responseCode == BillingClient.BillingResponseCode.OK

    companion object {
        /** Goals allowed without Plus. Nobody loses goals they already have. */
        const val FREE_GOALS = 3
    }
}
