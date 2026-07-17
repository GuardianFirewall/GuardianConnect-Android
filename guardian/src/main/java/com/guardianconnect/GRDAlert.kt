package com.guardianconnect

import com.google.gson.internal.LazilyParsedNumber

class GRDAlert {
	var action: 		String? = null
	var category: 		String? = null
	var host: 			String? = null
	var identifier:		String? = null
	var message: 		String? = null
	var timestamp:		Long? = null
	var title: 			String? = null

	companion object {
		fun alertFromMap(map: Map<String, Any>): GRDAlert {
			val newAlert = GRDAlert()
			newAlert.action = map["action"] as String
			newAlert.category = map["category"] as String
			newAlert.host = map["host"] as String
			newAlert.identifier = map["uuid"] as String
			newAlert.message = map["message"] as String
			newAlert.timestamp = (map["timestamp"] as? LazilyParsedNumber)?.toLong() ?: 0L
			newAlert.title = map["title"] as String

			return newAlert
		}
	}
}


class GRDAlertCategory {
	var categoryName: 	String? = null
	var summaryCount: 	Long? = null
	var title: 			String? = null
}
