package com.guardianconnect

import kotlin.enums.enumEntries

enum class GRDClientRuleMatchType {
	Unknown,
	FQDN,
	IP;

	companion object {
		fun from(value: Any?): GRDClientRuleMatchType {
			if (value is Int) {
				return entries[value as Int]

			} else if (value is Long) {
				return entries[value.toInt()]
			}

			return Unknown
		}

		fun forString(value: String): GRDClientRuleMatchType {
			val uppercaseValue = value.uppercase()
			if (uppercaseValue == "FQDN") {
				return FQDN

			} else if (uppercaseValue == "IP") {
				return IP
			}

			return Unknown
		}
	}

	fun toInt(): Int {
		if (this == FQDN) {
			return 1

		} else if (this == IP) {
			return 2
		}

		return 0
	}

	override fun toString(): String {
		if (this == FQDN) {
			return "FQDN"

		} else if (this == IP) {
			return "IP"
		}

		return "UNKNOWN"
	}
}

enum class GRDClientRuleVerdict {
	Unknown,
	Allow,
	Block,
	Default;

	companion object {
		fun from(value: Any?): GRDClientRuleVerdict {
			if (value is Int) {
				return entries[value as Int]

			} else if (value is Long) {
				return entries[value.toInt()]
			}

			return Unknown
		}

		fun forString(value: String): GRDClientRuleVerdict {
			val uppercaseValue = value.uppercase()
			if (uppercaseValue == "ALLOW") {
				return Allow

			} else if (uppercaseValue == "BLOCK") {
				return Block

			} else if (uppercaseValue == "DEFAULT") {
				return Default
			}

			return Unknown
		}
	}

	fun toInt(): Int {
		if (this == Allow) {
			return 1

		} else if (this == Block) {
			return 2

		} else if (this == Default) {
			return 3
		}

		return 0
	}

	override fun toString(): String {
		if (this == Allow) {
			return "Allow"

		} else if (this == Block) {
			return "Block"

		} else if (this == Default) {
			return "Default"
		}

		return "Unknown"
	}
}

class GRDClientRule {
	var matchType: GRDClientRuleMatchType? = GRDClientRuleMatchType.Unknown
	var matchPort: String? = null
	var matchValue: String? = null
	var ruleId: 	Int? = 0
	var verdict: 	GRDClientRuleVerdict? = GRDClientRuleVerdict.Unknown
	var multihopExitRegion: String? = "disabled"
	var enabled: 	Boolean? = false


	companion object {
		fun initFromMap(map: Map<String, Any>): GRDClientRule {
			val clientRule = GRDClientRule()
			val matchType 			= GRDClientRuleMatchType.from(map["match-type"])
			clientRule.matchType 	= matchType

			clientRule.matchPort 	= map["match-port"] as String?
			clientRule.matchValue 	= map["match-value"] as String?
			clientRule.ruleId 		= (map["rule-id"] as Long).toInt()

			val verdict = GRDClientRuleVerdict.from(map["verdict"])
			clientRule.verdict 		= verdict

			clientRule.multihopExitRegion = map["multihop-exit-region"] as String?
			clientRule.enabled 		= (map["enabled"] as Long) == 1L

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
		map["rule-id"] 				= this.ruleId as Int
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
