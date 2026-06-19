package com.guardianconnect

enum class GRDClientRuleMatchType {
	Unknown,
	FQDN,
	IP;

	fun toInt(): Int {
		if (this == GRDClientRuleMatchType.FQDN) {
			return 1

		} else if (this == GRDClientRuleMatchType.IP) {
			return 2
		}

		return 0
	}
}

enum class GRDClientRuleVerdict {
	Unknown,
	Allow,
	Block,
	Default;

	fun toInt(): Int {
		if (this == GRDClientRuleVerdict.Allow) {
			return 1

		} else if (this == GRDClientRuleVerdict.Block) {
			return 2

		} else if (this == GRDClientRuleVerdict.Default) {
			return 3
		}

		return 0
	}
}

class GRDClientRule {
	var matchType: GRDClientRuleMatchType? = GRDClientRuleMatchType.Unknown
	var matchPort: String? = null
	var matchValue: String? = null
	var ruleId: Int? = null
	var verdict: GRDClientRuleVerdict? = GRDClientRuleVerdict.Unknown
	var multihopExitRegion: String? = null
	var enabled: Boolean? = false


	companion object {
		fun initFromMap(map: Map<String, Any>): GRDClientRule {
			val clientRule = GRDClientRule()
			clientRule.matchType = map["match-type"] as GRDClientRuleMatchType?
			clientRule.matchPort = map["match-port"] as String?
			clientRule.matchValue = map["match-value"] as String?
			clientRule.ruleId = map["rule-id"] as Int?
			clientRule.verdict = map["verdict"] as GRDClientRuleVerdict?
			clientRule.multihopExitRegion = map["multihop-exit-region"] as String?
			clientRule.enabled = map["enabled"] as Boolean?

			return clientRule
		}


		fun allMatchTypes(): Array<GRDClientRuleMatchType> {
			return arrayOf(GRDClientRuleMatchType.FQDN, GRDClientRuleMatchType.IP)
		}

		fun titleForMatchType(type: GRDClientRuleMatchType): String {
			if (type == GRDClientRuleMatchType.FQDN) {
				return "FQDN"

			} else if (type == GRDClientRuleMatchType.IP) {
				return "IP"
			}

			return "UNKNOWN"
		}

		fun keyForMatchType(type: GRDClientRuleMatchType): String {
			if (type == GRDClientRuleMatchType.FQDN) {
				return "fqdn"

			} else if (type == GRDClientRuleMatchType.IP) {
				return "ip"
			}

			return "unknown"
		}

		fun allVerdicts(): Array<GRDClientRuleVerdict> {
			return arrayOf(GRDClientRuleVerdict.Allow, GRDClientRuleVerdict.Block, GRDClientRuleVerdict.Default)
		}

		fun titleForVerdict(verdict: GRDClientRuleVerdict): String {
			if (verdict == GRDClientRuleVerdict.Allow) {
				return "ALLOW"

			} else if (verdict == GRDClientRuleVerdict.Block) {
				return "BLOCK"

			} else if (verdict == GRDClientRuleVerdict.Default) {
				return "DEFAULT"
			}

			return "UNKNOWN"
		}

		fun keyForVerdict(verdict: GRDClientRuleVerdict): String {
			if (verdict == GRDClientRuleVerdict.Allow) {
				return "allow"

			} else if (verdict == GRDClientRuleVerdict.Block) {
				return "block"

			} else if (verdict == GRDClientRuleVerdict.Default) {
				return "default"
			}

			return "unknown"
		}
	}

	fun encodeToMap(): Map<String, Any> {
		val map = mutableMapOf<String, Any>()
		map["match-type"] 			= this.matchType?.toInt() as Int
		map["match-port"] 			= this.matchPort.toString()
		map["match-value"] 			= this.matchValue.toString()
		map["rule-id"] 				= this.ruleId?.toInt() as Int
		map["verdict"]				= this.verdict?.toInt() as Int
		map["multihop-exit-region"] = this.multihopExitRegion.toString()
		map["enabled"]				= this.enabled!!.compareTo(false)

		return map
	}

	override fun equals(other: Any?): Boolean {
		val otherRule = other as GRDClientRule
		val same = (this.matchType == otherRule.matchType && this.matchPort.equals(otherRule.matchPort) && this.matchValue.equals(otherRule.matchValue) && this.verdict == otherRule.verdict)
		return same
	}
}
