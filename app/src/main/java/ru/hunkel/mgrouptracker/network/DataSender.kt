package ru.hunkel.mgrouptracker.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

const val TAG = "DataSender"

class DataSender(private val context: Context) {

    companion object {
        @JvmStatic
        fun isNetworkConnected(context: Context): Boolean {
            val mConnectivityManager: ConnectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            var result = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mConnectivityManager.getNetworkCapabilities(mConnectivityManager.activeNetwork)
                    ?.run {
                        result = when {
                            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                            else -> false
                        }
                    }
            } else {
                mConnectivityManager.activeNetworkInfo.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        else -> false
                    }
                }
            }
            return result
        }
    }

    fun sendPunchesAsync(jsonPunches: String, uploadUrl: String): Deferred<Boolean> =
        CoroutineScope(Dispatchers.Default).async {
            var sended = false
            try {
                val mConnection = URL(uploadUrl).openConnection() as HttpURLConnection
                Log.i(TAG, "=====================================================")
                Log.i(TAG, "upload url: ${mConnection.url}")
                val job = runBlocking {
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
                            val result = runBlocking {
                                handleCorrectResponse(mConnection.inputStream)
                            }
                            if (result) {
                                sended = true
                            }
                            Log.i(TAG, "response message: $result")
                        }
                        else -> {
                            val result = runBlocking {
                                handleIncorrectResponse(mConnection.errorStream)
                            }
                            Log.i(TAG, "response message: $result")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in uploadPoints", e) //NON-NLS
            }
            sended
        }

    private fun handleCorrectResponse(stream: InputStream): Boolean {
        var msg = ""
        for (i in stream.readBytes()) {
            msg += i.toChar()
        }
        val json = responseToJson(msg)
        var success = false
        when (json.getString("status")) {
            "ok" -> {
                success = true
            }
        }
        return success
    }

    private fun handleIncorrectResponse(stream: InputStream): String {
        var msg = ""
        for (i in stream.readBytes()) {
            msg += i.toChar()
        }
        Log.i(TAG, msg)
        return msg
    }

    private fun responseToJson(string: String): JSONObject {
        return JSONObject(string)
    }
}