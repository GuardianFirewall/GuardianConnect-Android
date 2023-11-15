package com.guardianconnect.util

import com.guardianconnect.GRDConnectManager

object GRDLogger {

    fun allLogs(): ArrayList<String> {
        val logs = GRDConnectManager
            .getSharedPrefs()?.getString(Constants.GRD_PERSISTENT_LOG, "")
        return logs?.split(",") as ArrayList<String>
    }

    fun allLogsFormatted(): String {
        val logsArray = allLogs()
        return logsArray.joinToString("\n") { it }
    }

    fun deleteAllLogs() {
        GRDConnectManager.getSharedPrefsEditor()?.remove(Constants.GRD_PERSISTENT_LOG)?.apply()
    }
}