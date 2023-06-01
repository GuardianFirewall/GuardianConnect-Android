package com.guardianconnect.api

interface IOnApiResponse {

    fun onSuccess(any: Any?)

    fun onError(error: String?)
}