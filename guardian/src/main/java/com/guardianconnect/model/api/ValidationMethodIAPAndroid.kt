package com.guardianconnect.model.api

import com.google.gson.annotations.SerializedName

class ValidationMethodIAPAndroid {

    @SerializedName("validation-method")
    var validationMethod: String? = null

    @SerializedName("bundle-id")
    var bundleId: String? = null

    @SerializedName("purchase-token")
    var purchaseToken: String? = null

    @SerializedName("product-id")
    var productId: String? = null

    @SerializedName("product-type")
    var productType: String? = null

}