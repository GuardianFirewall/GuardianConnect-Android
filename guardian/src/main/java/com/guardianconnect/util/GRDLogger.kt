package com.guardianconnect.util

import android.util.Log
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.util.Constants.Companion.GRD_PERSISTENT_LOG_ENABLED

object GRDLogger {

    fun allLogs(): List<String>? {
        val logs = GRDConnectManager
            .getSharedPrefs().getString(Constants.GRD_PERSISTENT_LOG, "")
        return logs?.split(",")
    }

    fun allLogsFormatted(): String? {
        val logsArray = allLogs()
        return logsArray?.joinToString("\n") { it }
    }

    fun togglePersistentLogging(logEnabled: Boolean) {
        GRDConnectManager.getSharedPrefsEditor().putBoolean(GRD_PERSISTENT_LOG_ENABLED, logEnabled)
            ?.apply()
    }

    private fun iskGRDPersistentLogEnabled(): Boolean {
        return GRDConnectManager.getSharedPrefs().getBoolean(GRD_PERSISTENT_LOG_ENABLED, false)
    }

    fun zzz_log(logPriority: Int, tag: String, message: String, b: Boolean) {
        val arrayOfStrings = allLogs()?.let { ArrayList<String>(it) }
        if(arrayOfStrings != null)
            if (arrayOfStrings.size > 199) {
                if (!b && iskGRDPersistentLogEnabled()) {
                    arrayOfStrings.removeAt(0)
                    arrayOfStrings += message
                    val messagesAsString = arrayOfStrings.joinToString("\n") { it }
                    GRDConnectManager.getSharedPrefsEditor()
                        .putString(GRD_PERSISTENT_LOG_ENABLED, messagesAsString)
                        .apply()
                }
            }
        Log.println(logPriority, tag, message)
    }

    fun d(tag: String, message: String) {
        zzz_log(Log.DEBUG, tag, message, false)
    }

    fun w(tag: String, message: String) {
        zzz_log(Log.WARN, tag, message, false)
    }

    fun e(tag: String, message: String) {
        zzz_log(Log.ERROR, tag, message, false)
    }

    fun deleteAllLogs() {
        GRDConnectManager.getSharedPrefsEditor().remove(Constants.GRD_PERSISTENT_LOG)?.apply()
    }
}