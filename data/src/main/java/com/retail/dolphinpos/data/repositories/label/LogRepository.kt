package com.retail.dolphinpos.data.repositories.label

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val logFile: File = File(context.filesDir, "print_logs.txt")

    fun logToFile(message: String) {
        try {
            logFile.appendText("${System.currentTimeMillis()}: $message\n")
        } catch (e: Exception) {
            logFile.appendText("Error writing log: ${e.localizedMessage}\n")
        }
    }

    fun readLogs(): String {
        return try {
            logFile.readText()
        } catch (e: Exception) {
            "Error reading log file: ${e.localizedMessage}"
        }
    }
}
