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

            val job = GlobalScope.launch {
                mConnection.setRequestProperty("Accept-Charset", "UTF-8")
                mConnection.requestMethod = "POST"
                mConnection.doInput = true
                mConnection.doOutput = true

                val os = mConnection.outputStream
                os.write(jsonPunches.toByteArray(Charsets.UTF_8))
                mConnection.connect()
                Log.i("RRR", mConnection.responseCode.toString())

                val result = async {
                    mConnection.inputStream
                }
                handleData(result.await())
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
//            Log.i(TAG, i)
        }
        Log.i(TAG, msg)
        return msg
    }
}