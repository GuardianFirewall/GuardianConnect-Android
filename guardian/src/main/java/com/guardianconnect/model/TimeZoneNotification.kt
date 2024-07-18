package com.guardianconnect.model

data class TimeZoneNotification(
    var changed: Boolean? = null,
    var oldTimezoneName: String? = null,
    var newTimezoneName: String? = null
)