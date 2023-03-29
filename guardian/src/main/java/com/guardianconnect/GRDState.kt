package com.guardianconnect

enum class GRDState(state: String) {
    SERVER_READY("Server status OK."),
    SERVER_ERROR("Server error!"),
    TUNNEL_CONNECTED("Connection Successful!")
}