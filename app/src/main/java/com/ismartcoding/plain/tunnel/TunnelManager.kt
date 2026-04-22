package com.ismartcoding.plain.tunnel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object TunnelManager {
    private const val TOKEN = "eyJhIjoiNzk4MDRjYzVhNTdhMGFjZTVkZDA4NmZhMDdkOTc2NTAiLCJ0IjoiNDI3MzQyYjMtODU4MS00ZjMxLThiYjctN2UzOGViYWEwMzI3IiwicyI6Ik5qUmtPVEk1TnpNdE9UVTNZUzAwWVRCaUxXSmlPV1F0T1RnNVpESXdaalU0WkdZMiJ9"

    private var process: Process? = null
    private var job: Job? = null
    private var isRunning = false

    val isTunnelRunning: Boolean
        get() = isRunning && process?.isAlive == true

    fun startTunnel(context: Context): Boolean {
        if (isTunnelRunning) {
            LogCat.d("Tunnel is already running")
            return true
        }

        val binaryFile = AssetExtractor.extractBinary(context)
        if (binaryFile == null) {
            LogCat.e("Failed to extract cloudflared binary")
            return false
        }

        return try {
            val processBuilder = ProcessBuilder(
                binaryFile.absolutePath,
                "tunnel",
                "run",
                "--token",
                TOKEN
            ).apply {
                redirectErrorStream(true)
            }

            process = processBuilder.start()
            isRunning = true

            // Start monitoring job
            job = CoroutineScope(Dispatchers.IO).launch {
                monitorProcess()
            }

            // Start foreground service
            val intent = Intent(context, TunnelService::class.java)
            ContextCompat.startForegroundService(context, intent)

            LogCat.d("Cloudflare tunnel started")
            true
        } catch (e: IOException) {
            LogCat.e("Failed to start tunnel process: ${e.message}")
            isRunning = false
            false
        }
    }

    fun stopTunnel() {
        job?.cancel()
        job = null

        process?.destroy()
        process = null
        isRunning = false

        LogCat.d("Cloudflare tunnel stopped")
    }

    private suspend fun monitorProcess() {
        val proc = this.process ?: return

        try {
            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    LogCat.d("Cloudflared: $line")
                    line = reader.readLine()
                }
            }
        } catch (e: IOException) {
            LogCat.e("Error reading tunnel output: ${e.message}")
        }

        val exitCode = proc.waitFor()
        LogCat.d("Tunnel process exited with code: $exitCode")

        isRunning = false
    }
}