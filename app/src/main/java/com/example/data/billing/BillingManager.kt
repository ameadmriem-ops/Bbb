package com.example.data.billing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.data.repository.AuthRepository
import com.example.data.repository.UsageQuotaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI representation of a Google Play Billing product with genuine Play Store pricing and metadata.
 */
data class PlayBillingProduct(
    val productId: String,
    val title: String,
    val formattedPrice: String,
    val description: String,
    val isSubscription: Boolean,
    val productDetails: ProductDetails,
    val offerToken: String? = null
)

/**
 * Robust manager for Google Play Billing v7 integration.
 * Handles subscriptions, consumables (credits), acknowledgment, consumption, and restore flows.
 */
class BillingManager(
    private val context: Context,
    private val usageQuotaManager: UsageQuotaManager,
    private val authRepository: AuthRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _products = MutableStateFlow<Map<String, PlayBillingProduct>>(emptyMap())
    val products: StateFlow<Map<String, PlayBillingProduct>> = _products.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _billingMessage = MutableStateFlow<String?>(null)
    val billingMessage: StateFlow<String?> = _billingMessage.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    companion object {
        const val TAG = "BillingManager"

        // Subscriptions
        const val PRODUCT_PRO_MONTHLY = "ai_pro_monthly"
        const val PRODUCT_PRO_YEARLY = "ai_pro_yearly"

        // In-app consumable credits
        const val PRODUCT_CREDITS_100 = "credits_100"
        const val PRODUCT_CREDITS_500 = "credits_500"
        const val PRODUCT_CREDITS_1000 = "credits_1000"
    }

    init {
        startConnection()
    }

    fun startConnection() {
        if (billingClient.isReady) {
            queryAllProducts()
            restorePurchases()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing setup successful.")
                    _isConnected.value = true
                    queryAllProducts()
                    restorePurchases()
                } else {
                    Log.w(TAG, "Billing setup failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                    _isConnected.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Will reconnect on next action.")
                _isConnected.value = false
            }
        })
    }

    /**
     * Query all required subscription and credit products from Google Play Console.
     */
    fun queryAllProducts() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }

        val subList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val inAppList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_CREDITS_100)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_CREDITS_500)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_CREDITS_1000)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val resultMap = mutableMapOf<String, PlayBillingProduct>()

        // 1. Query Subscriptions
        val subParams = QueryProductDetailsParams.newBuilder().setProductList(subList).build()
        billingClient.queryProductDetailsAsync(subParams) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                detailsList.forEach { details ->
                    val offer = details.subscriptionOfferDetails?.firstOrNull()
                    val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    val price = phase?.formattedPrice ?: "غير محدد"
                    resultMap[details.productId] = PlayBillingProduct(
                        productId = details.productId,
                        title = details.title.substringBefore("(").trim(),
                        formattedPrice = price,
                        description = details.description,
                        isSubscription = true,
                        productDetails = details,
                        offerToken = offer?.offerToken
                    )
                }
            }

            // 2. Query In-App Consumable Credits
            val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppList).build()
            billingClient.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsList ->
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    inAppDetailsList.forEach { details ->
                        val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: "غير محدد"
                        resultMap[details.productId] = PlayBillingProduct(
                            productId = details.productId,
                            title = details.title.substringBefore("(").trim(),
                            formattedPrice = price,
                            description = details.description,
                            isSubscription = false,
                            productDetails = details,
                            offerToken = null
                        )
                    }
                }
                _products.value = resultMap
            }
        }
    }

    /**
     * Launch Google Play Billing Purchase BottomSheet.
     */
    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val product = _products.value[productId]
        if (product == null) {
            _billingMessage.value = "المنتج غير متاح حالياً من Google Play"
            return
        }

        if (!billingClient.isReady) {
            _billingMessage.value = "جارٍ الاتصال بمتجر Google Play..."
            startConnection()
            return
        }

        _isPurchasing.value = true

        val productDetailsParamsList = if (product.isSubscription) {
            val token = product.offerToken ?: product.productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product.productDetails)
                    .setOfferToken(token)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product.productDetails)
                    .build()
            )
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).responseCode
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            _isPurchasing.value = false
            _billingMessage.value = "تعذر بدء عملية الشراء من Google Play"
        }
    }

    /**
     * Callback from Google Play Billing upon purchase completion or change.
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isPurchasing.value = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled Google Play purchase flow.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _billingMessage.value = "الاشتراك مفعّل بالفعل على هذا الحساب"
                restorePurchases()
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _billingMessage.value = "فشلت عملية الشراء: ${billingResult.debugMessage}"
            }
        }
    }

    /**
     * Handles subscription acknowledgment and consumable credits consumption.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        scope.launch {
            val productIds = purchase.products
            for (productId in productIds) {
                when (productId) {
                    PRODUCT_PRO_MONTHLY, PRODUCT_PRO_YEARLY -> {
                        // Subscriptions must be acknowledged
                        if (!purchase.isAcknowledged) {
                            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                            billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                    activateProSubscription(productId)
                                }
                            }
                        } else {
                            activateProSubscription(productId)
                        }
                    }

                    PRODUCT_CREDITS_100, PRODUCT_CREDITS_500, PRODUCT_CREDITS_1000 -> {
                        // Consumable credits must be consumed to be credited and repurchased
                        val consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient.consumeAsync(consumeParams) { result, _ ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                val creditsToAdd = when (productId) {
                                    PRODUCT_CREDITS_100 -> 100
                                    PRODUCT_CREDITS_500 -> 500
                                    PRODUCT_CREDITS_1000 -> 1000
                                    else -> 0
                                }
                                creditUserBalance(creditsToAdd)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Restores existing active subscriptions and consumes any pending credits.
     */
    fun restorePurchases(onComplete: ((Boolean) -> Unit)? = null) {
        if (!billingClient.isReady) {
            startConnection()
            onComplete?.invoke(false)
            return
        }

        // 1. Query active subscriptions
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(subsParams) { result, purchases ->
            var hasActiveSub = false
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        for (prodId in purchase.products) {
                            if (prodId == PRODUCT_PRO_MONTHLY || prodId == PRODUCT_PRO_YEARLY) {
                                hasActiveSub = true
                                handlePurchase(purchase)
                            }
                        }
                    }
                }
            }

            // 2. Query in-app purchases to consume unconsumed credits
            val inAppParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()

            billingClient.queryPurchasesAsync(inAppParams) { inAppResult, inAppPurchases ->
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (purchase in inAppPurchases) {
                        handlePurchase(purchase)
                    }
                }
                onComplete?.invoke(hasActiveSub)
            }
        }
    }

    /**
     * Activates Pro status in both UsageQuotaManager and UserProfile/Firebase.
     */
    private fun activateProSubscription(planId: String) {
        scope.launch {
            usageQuotaManager.setPremium(true, planId)
            val current = authRepository.currentUser.value
            if (current != null) {
                authRepository.updateProfile(
                    displayName = current.displayName,
                    isPremium = true
                )
            }
            _billingMessage.value = "تهانينا! تم تفعيل اشتراك AI Smart Premium بنجاح 🎉"
        }
    }

    /**
     * Adds purchased credits safely after Google Play consumption verification.
     */
    private fun creditUserBalance(amount: Int) {
        scope.launch {
            usageQuotaManager.addCredits(amount)
            _billingMessage.value = "تمت إضافة $amount نقطة (Credits) إلى رصيدك بنجاح 💎"
        }
    }

    /**
     * Deep links to Google Play subscriptions management screen.
     */
    fun openManageSubscriptions(activity: Activity) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions?package=${activity.packageName}")
            )
            activity.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/account/subscriptions")
            )
            activity.startActivity(intent)
        }
    }

    fun clearMessage() {
        _billingMessage.value = null
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
