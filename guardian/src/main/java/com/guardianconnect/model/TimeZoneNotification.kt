package com.guardianconnect.model

import com.guardianconnect.GRDRegion

data class TimeZoneNotification(
    var changed: Boolean? = null,
    var oldTimezoneName: GRDRegion? = null,
    var newTimezoneName: GRDRegion? = null
)