package com.guardianconnect.enumeration

enum class GRDServerFeatureEnvironment {
    ServerFeatureEnvironmentProduction,
    ServerFeatureEnvironmentInternal,
    ServerFeatureEnvironmentDevelopment,
    ServerFeatureEnvironmentDualStack,
    ServerFeatureEnvironmentUnstable;

    companion object {
        fun defaultValue(): GRDServerFeatureEnvironment {
            return ServerFeatureEnvironmentProduction
        }
    }
}
