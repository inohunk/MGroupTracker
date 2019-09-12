package ru.hunkel.mgrouptracker.network

import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL

const val TAG = "DataSender"

class DataSender {

    fun uploadPunches(jsonPunches: String, uploadUrl: String) {
        try {
            val mConnection = URL(uploadUrl).openConnection() as HttpURLConnection
            Log.i(TAG, "=====================================================")
            Log.i(TAG, "upload url: ${mConnection.url}")
            val job = GlobalScope.launch {
                mConnection.requestMethod = "POST"
                mConnection.setRequestProperty("Accept-Charset", "UTF-8")
                mConnection.setRequestProperty("Content-Type", "application/json")
                mConnection.setRequestProperty("Connection", "Keep-Alive");
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
                        Log.i(TAG, result.await())
                    }
                    else -> {
                        val result = async {
                            handleData(mConnection.errorStream)
                        }
                        Log.i(TAG, result.await())

                    }
                }


            }
            CoroutineScope(Dispatchers.Default).launch {
                job.join()
            }
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "UnsupportedEncodingException in uploadPoints", e) //NON-NLS
        } catch (e: IOException) {
            Log.e(TAG, "UnsupportedEncodingException in uploadPoints", e) //NON-NLS
        } catch (e: Exception) {
            Log.e(TAG, "Exception in uploadPoints", e) //NON-NLS
        }
    }

    private fun handleData(inStream: InputStream): String {
        var msg = ""
        for (i in inStream.readBytes()) {
            msg += i.toChar()
        }
        Log.i(TAG, "ogps response message: $msg")
        return msg
    }
}