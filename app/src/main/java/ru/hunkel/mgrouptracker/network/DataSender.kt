package ru.hunkel.mgrouptracker.network

import android.util.Log
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

const val TAG = "DataSender"

class DataSender {

    fun sendPunches(jsonPunches: String, uploadUrl: String) {
        try {
            val mConnection = URL(uploadUrl).openConnection() as HttpURLConnection
            Log.i(TAG, "=====================================================")
            Log.i(TAG, "upload url: ${mConnection.url}")
            val job = GlobalScope.launch {
                mConnection.requestMethod = "POST"
                mConnection.setRequestProperty("Accept-Charset", "UTF-8")
                mConnection.setRequestProperty("Content-Type", "application/json")

                mConnection.doInput = true
                mConnection.doOutput = true

                mConnection.connect()
                Log.i(TAG, "connect success")
                mConnection.outputStream.use {
                    val input = jsonPunches.toByteArray(Charsets.UTF_8)
                    it.write(input, 0, input.size)
                }

                Log.i(TAG, "response code: ${mConnection.responseCode}")
                when (mConnection.responseCode) {
                    200, 201 -> {
                        val result = async {
                            handleData(mConnection.inputStream)
                        }
                        Log.i(TAG, "response message: ${result.await()}")
                    }
                    else -> {
                        val result = async {
                            handleData(mConnection.errorStream)
                        }
                        Log.i(TAG, "response message: ${result.await()}")
                    }
                }
            }
            CoroutineScope(Dispatchers.Default).launch {
                job.join()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadPoints", e) //NON-NLS
        }
    }

    private fun handleData(stream: InputStream): String {
        var msg = ""
        for (i in stream.readBytes()) {
            msg += i.toChar()
        }
        Log.i(TAG, "")
        return msg
    }
}