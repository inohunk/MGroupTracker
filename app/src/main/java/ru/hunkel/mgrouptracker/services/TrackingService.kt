package ru.hunkel.mgrouptracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import org.altbeacon.beacon.*
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.activities.MainActivity
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.utils.STATE_OFF
import ru.hunkel.mgrouptracker.utils.STATE_ON
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.system.exitProcess


class TrackingService : Service(), BeaconConsumer {


    companion object {
        private const val WAKE_LOCK_TAG = "PunchingApp:WakeLockTag"
        private const val LOCK_UPDATE_INTERVAL: Long = 120000 // 120sec 2min
    }

    private val TAG = "TrackingService"

    private var mWakeLock: PowerManager.WakeLock? = null
    private var mLastLockUpdateMillis: Long = 0
    private lateinit var mBeaconManager: BeaconManager
    private var mBuilder: NotificationCompat.Builder? = null
    private val CHANNEL_ID = "TrackingNotifications"
    private var mCnt = 0
    private val mPunches = LinkedList<Int>()

    private var mTrackingState = STATE_OFF

    /*
        VARIABLES
    */
    private lateinit var mDatabaseManager: DatabaseManager

    /*
        INNER CLASSES
    */
    inner class TrackingServiceImpl : ITrackingService.Stub() {
        override fun startEvent() {
            mDatabaseManager.actionStartEvent()
            startOnClick()
        }

        override fun punch() {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun stopEvent() {
            mDatabaseManager.actionStopEvent()
            stopOnClick()
        }

        override fun getState(): Int {
            return this@TrackingService.mTrackingState
        }
    }

    /*
        FUNCTIONS
    */
    override fun onCreate() {
        mDatabaseManager = DatabaseManager(this)

        initBeaconManager()
    }

    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }

    override fun onDestroy() {
        stopOnClick()
        exitProcess(1)
    }

    private fun initBeaconManager() {
        mBeaconManager = BeaconManager.getInstanceForApplication(this)
        mBeaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"))

        mBuilder = NotificationCompat.Builder(this,CHANNEL_ID)
        mBuilder!!.setSmallIcon(R.drawable.notification_icon_background)
        mBuilder!!.setContentTitle("Scanning for Controls")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        mBuilder!!.setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Controls Notification Channel",
                "Controls Notification", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Controls Notification Channel"
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channel.id)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mBeaconManager.enableForegroundServiceScanning(mBuilder!!.build(), 456)
        }
        mBeaconManager.setEnableScheduledScanJobs(false)
    }

    override fun onBeaconServiceConnect() {
        mBeaconManager.removeAllRangeNotifiers()
        mBeaconManager.addRangeNotifier { beacons, region ->
            updateWakeLock()

            if (beacons.isNotEmpty()) {
                mCnt++
                val it = beacons.iterator()
                var bcn: Beacon?
                var mnr: Int
                var i: Int
                var totalPoints = 0


                while (it.hasNext()) {
                    bcn = it.next()
                    if (bcn == null || Integer.parseInt(bcn.id2.toString()) != 0xCDBF) continue
                    mnr = Integer.parseInt(bcn.id3.toString())

                    if (mPunches.size > 0) {
                        i = 0
                        while (i < mPunches.size) {
                            if (mPunches[i] === mnr) {
                                mnr = -1
                                break
                            }
                            i++
                        }
                    }

                    if (mnr != -1) mPunches.add(mnr)
                }

                var s = "*** $mCnt ***"
                if (mPunches.size > 0) {
                    i = 0
                    while (i < mPunches.size) {
                        s += "\n" + mPunches[i]
                        totalPoints += floor((mPunches[i] / 10).toDouble()).toInt()
                        i++
                    }
                }
                s += "\n--------------\n$totalPoints"

                Log.i(TAG, s)
            }
        }

        try {
            mBeaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
        } catch (e: RemoteException) {
            Log.e(TAG, e.message)
        }

    }

    fun startOnClick() {
        stopOnClick()

        mCnt = 0
        mPunches.clear()
        mBeaconManager.bind(this)
        mTrackingState = STATE_ON

        updateWakeLock()
    }

    fun stopOnClick() {
        if (mBeaconManager.isBound(this))
            mBeaconManager.unbind(this)

        mTrackingState = STATE_OFF

        updateWakeLock()

    }

    private fun updateWakeLock() {
        if (mBeaconManager.isBound(this)) {
            val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
            val currentMillis = System.currentTimeMillis()
            if (abs(currentMillis - mLastLockUpdateMillis) > LOCK_UPDATE_INTERVAL) {
                mLastLockUpdateMillis = currentMillis

                if (this.mWakeLock != null) {
                    this.mWakeLock!!.release()
                    this.mWakeLock = null
                }

                this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                this.mWakeLock!!.acquire()
            }
        } else {
            mLastLockUpdateMillis = 0
            if (this.mWakeLock != null) {
                this.mWakeLock!!.release()
                this.mWakeLock = null
            }
        }
    }
}
