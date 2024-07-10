package com.guardianconnect.billing

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.guardianconnect.helpers.GRDVPNHelper
import com.guardianconnect.managers.GRDConnectManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GRDBillingManager {

    private lateinit var purchase: Purchase
    private val mProductDetailsList = ArrayList<ProductDetails>()
    private val fragmentTag = "com.guardian.billing.BillingManager"

    val productDetailsList: List<ProductDetails>
        get() = mProductDetailsList

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            Log.d(fragmentTag, "Product: " + billingResult.responseCode)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
                && purchases != null
            ) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode ==
                BillingClient.BillingResponseCode.USER_CANCELED
            ) {
                Log.d(fragmentTag, "Purchase Canceled")
            } else {
                Log.d(fragmentTag, "Purchase Error")
            }
        }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val billingClient: BillingClient =
        BillingClient.newBuilder(GRDConnectManager.get().getContext())
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(
                        fragmentTag,
                        "startConnection responseCode: " + billingResult.responseCode
                    )
                    GRDVPNHelper.allowedProductIds?.let {
                        for (productId in it) {
                            queryProduct(productId)
                        }
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }


    private fun queryProduct(productId: String) {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (productDetailsList.isNotEmpty()) {
                mProductDetailsList.addAll(productDetailsList) // Add the fetched product details to the list

                val productDetails = productDetailsList[0]
                Log.d(fragmentTag, "Product: " + productDetails.name)
            } else {
                Log.d(fragmentTag, "No Matching Products Found ")
            }
        }
    }

    fun launchPurchaseFlow(productDetails: ProductDetails, activity: AppCompatActivity) {
        val subOfferDetails = productDetails.subscriptionOfferDetails
        val selectedOfferToken = subOfferDetails!![0].offerToken
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(selectedOfferToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        // Launch the billing flow
        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        Log.d(fragmentTag, "billingResult " + billingResult.responseCode)
    }

    private fun handlePurchase(item: Purchase) {
        purchase = item
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                coroutineScope.launch {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                }
            } else {
                Log.d(fragmentTag, "Purchase Completed")
                consumePurchase()
            }
        }
    }

    private fun consumePurchase() {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        coroutineScope.launch {
            val result = billingClient.consumePurchase(consumeParams)

            if (result.billingResult.responseCode ==
                BillingClient.BillingResponseCode.OK
            ) {
                Log.d(fragmentTag, "Purchase Consumed")
            } else {
                Log.e(fragmentTag, "Purchase Consume fail")
            }
        }
    }

    fun getCurrentPurchase(): Purchase? {
        return if (::purchase.isInitialized) {
            purchase
        } else {
            null
        }
    }

    fun isSubscription(purchase: Purchase): Boolean {
        return GRDVPNHelper.allowedProductIds?.any { purchase.products.contains(it) } == true
    }
}