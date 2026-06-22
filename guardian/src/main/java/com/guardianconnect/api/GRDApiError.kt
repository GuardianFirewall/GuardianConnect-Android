package com.guardianconnect.api

import com.google.gson.Gson
import com.guardianconnect.util.Constants.Companion.APITYPETOKENMAP
import com.guardianconnect.util.GRDLogger
import okhttp3.ResponseBody
import retrofit2.Response

class GRDApiError {
	var title: 		String? = ""
	var message: 	String? = ""
	var statusCode: Int? = -1

	var rawErr: Map<String, Any>? = null

	companion object {
		fun apiErrorFromResponseBody(resp: Response<ResponseBody?>): GRDApiError {
			val apiErr = GRDApiError()
			apiErr.statusCode = resp.code()
			if (resp.body()?.string().isNullOrEmpty()) {
				apiErr.title 	= "Failed to Parse API Error"
				apiErr.message 	= "Failed to read the response body with HTTP status code: ${apiErr.statusCode}"
				return apiErr
			}

			try {
				val errorMap: Map<String, Any> = Gson().fromJson(resp.body()?.string(), APITYPETOKENMAP)
				apiErr.rawErr 	= errorMap
				apiErr.title 	= errorMap["error-title"] as String
				apiErr.message 	= errorMap["error-message"] as String

			} catch (e: Exception) {
				GRDLogger.e("GRDAPIError", "Failed to JSON decode response: $e")
				apiErr.title 	= "Failed to Parse API Error"
				apiErr.message 	= "Failed to JSON decode API error response status-code: ${apiErr.statusCode}; error: $e"
			}

			return apiErr
		}
	}

	override fun toString(): String {
		return "status-code: ${this.statusCode}; error-title: ${this.title}; error-message: ${this.message}"
	}
}
