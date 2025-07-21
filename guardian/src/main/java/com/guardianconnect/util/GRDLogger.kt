package com.guardianconnect.util

import java.util.Date
import android.util.Log
import java.text.SimpleDateFormat
import com.guardianconnect.managers.GRDConnectManager
import com.guardianconnect.util.Constants.Companion.GRD_PERSISTENT_LOG_ENABLED


object GRDLogger {
    fun allLogs(): List<String>? {
        val logs = GRDConnectManager.getSharedPrefs().getString(Constants.GRD_PERSISTENT_LOG, "")
        return logs?.split("\n")
    }

    fun allLogsFormatted(): String? {
        val logsArray = allLogs()
        return logsArray?.joinToString("\n") { it }
    }
    
    fun deleteAllLogs() {
        GRDConnectManager.getSharedPrefsEditor().remove(Constants.GRD_PERSISTENT_LOG)?.apply()
    }

    fun togglePersistentLogging(logEnabled: Boolean) {
        GRDConnectManager.getSharedPrefsEditor().putBoolean("kGRDPersistentLogEnabled", logEnabled)?.commit()
    }

    fun zzz_log(logPriority: Int, tag: String, message: String, preventPersistentLogging: Boolean) {
        val persistentLoggingEnabled = GRDConnectManager.getSharedPrefs().getBoolean(GRD_PERSISTENT_LOG_ENABLED, false)
        var arrayOfStrings = allLogs()
        if (arrayOfStrings.isNullOrEmpty()) {
            arrayOfStrings = listOf<String>()
        }
        if (persistentLoggingEnabled == true && preventPersistentLogging == false) {
            val mutableLogs = arrayOfStrings.toMutableList()
            if (mutableLogs.size > 199) {
                mutableLogs.removeAt(0)
            }

            var logLevel = ""
            if (logPriority == 3) {
                logLevel = " [DEBUG]"

            } else if (logPriority == 5) {
                logLevel = " [WARNING]"

            } else if (logPriority == 6) {
                logLevel = " [ERROR]"
            }

            val stack           = Thread.currentThread().stackTrace
            val className       = stack[4].className.split(".").last()
            val lineNumber      = stack[4].lineNumber
            val now             = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            val formattedLog    = "$now [$className:$lineNumber]$logLevel $message"

            mutableLogs += formattedLog
            val messagesAsString = mutableLogs.joinToString("\n") { it }
            GRDConnectManager.getSharedPrefsEditor().putString(Constants.GRD_PERSISTENT_LOG, messagesAsString).apply()
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

    fun i(tag: String, message: String) {
        zzz_log(Log.INFO, tag, message, false)
    }
}
