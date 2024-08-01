package com.guardianconnect.model

import com.guardianconnect.GRDRegion

data class TimeZoneNotification(
    var changed: Boolean? = null,
    var oldRegion: GRDRegion? = null,
    var newRegion: GRDRegion? = null
)