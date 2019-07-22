package ru.hunkel.mgrouptracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import org.altbeacon.beacon.*
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.activities.MainActivity
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.utils.STATE_OFF
import ru.hunkel.mgrouptracker.utils.STATE_ON
import java.util.*
import kotlin.math.abs


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

        checkBeaconSupport()
        initBeaconManager()
        startOnClick()
    }

    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }

    override fun onDestroy() {
        stopOnClick()
    }

    private fun initBeaconManager() {
        mBeaconManager = BeaconManager.getInstanceForApplication(this)
        mBeaconManager.backgroundMode = true
        mBeaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"))
        mBeaconManager.setEnableScheduledScanJobs(false)
    }

    private fun checkBeaconSupport() {
        when (BeaconTransmitter.checkTransmissionSupported(this)) {
            BeaconTransmitter.NOT_SUPPORTED_MIN_SDK -> Log.i(TAG, "CHECKING SUPPORT: not supported but min sdk")
            BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER,
            BeaconTransmitter.NOT_SUPPORTED_CANNOT_GET_ADVERTISER_MULTIPLE_ADVERTISEMENTS
            -> Log.i(TAG, "CHECKING SUPPORT: not supported but device have not api")
            BeaconTransmitter.NOT_SUPPORTED_BLE -> Log.i(TAG, "CHECKING SUPPORT: not supported but bluetooth problem")
            BeaconTransmitter.SUPPORTED -> Log.i(TAG, "CHECKING SUPPORT: supported")
        }
    }

    private fun createNotification() {
        mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        mBuilder!!.setSmallIcon(R.drawable.ic_running)
        mBuilder!!.setContentTitle("Scanning for Controls")
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        mBuilder!!.setContentIntent(pendingIntent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Controls Notification Channel",
                "Controls Notification", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Controls Notification Channel"
            notificationManager.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channel.id)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mBeaconManager.enableForegroundServiceScanning(mBuilder!!.build(), 456)
        } else {
            notificationManager.notify(1, mBuilder!!.build())
        }
    }


    private fun getBitmap(context: Context, drawableId: Int): Bitmap {
        var drawable = ContextCompat.getDrawable(context, drawableId)!!
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable).mutate()
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun createNotificationForControlPoint(controlPoint: Int) {
        mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        mBuilder!!.setSmallIcon(R.drawable.ic_control_point)
        mBuilder!!.setLargeIcon(getBitmap(this, R.drawable.ic_control_point))
        mBuilder!!.setContentTitle("Новый контрольный пункт!")
        mBuilder!!.setContentText("$controlPoint")
        mBuilder!!.setSound(Uri.parse("android.resource://ru.hunkel.mgrouptracker/" + R.raw.notification_sound))
        mBuilder!!.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(2, mBuilder!!.build())

    }

    override fun onBeaconServiceConnect() {
        mBeaconManager.removeAllRangeNotifiers()
        mBeaconManager.foregroundScanPeriod = 500
        mBeaconManager.backgroundBetweenScanPeriod = 0L
        mBeaconManager.backgroundScanPeriod = 500
        mBeaconManager.foregroundBetweenScanPeriod = 0L
        mBeaconManager.applySettings()

        val pm = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

        val distance = pm.getString("beacon_distance", "4.5")!!.toFloat()
        Log.i(TAG + "DISTANCE", distance.toString())
        mBeaconManager.addRangeNotifier { beacons, region ->
            try {
                updateWakeLock()
                if (beacons.isNotEmpty()) {
                    val iterator = beacons.iterator()

                    while (iterator.hasNext()) {
                        val beacon = iterator.next()
                        if (beacon.distance <= distance) {
                            checkInList(beacon.id3.toInt())
                        }
                        Log.i(
                            TAG, "FOUNDED BEACON:\n" +
                                    "\tid1: ${beacon.id1}\n" +
                                    "\tid2: ${beacon.id2}\n" +
                                    "\tid3: ${beacon.id3}\n" +
                                    "\tmanufacturer: ${beacon.manufacturer}\n" +
                                    "\ttxPower: ${beacon.txPower}\n" +
                                    "\trssi: ${beacon.rssi}\n" +
                                    "\tdistance: ${beacon.distance}\n"
                        )
                    }

                }
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
        }

        try {
            val major = Identifier.fromInt(0xCDBF)
            val region = Region(
                "cpSearch",
                null,
                major,
                null
            )
            mBeaconManager.startRangingBeaconsInRegion(region)

        } catch (e: RemoteException) {
            Log.e(TAG, e.message)
        }

    }

    private fun checkInList(cp: Int): Boolean {
        return if (mPunches.contains(cp)) {
            Log.i(TAG, "already exists in list")
            true
        } else {
            mPunches.add(cp)
            val punch = Punches(
                eventId = mDatabaseManager.actionGetLastEvent().id,
                time = System.currentTimeMillis(),
                controlPoint = cp
            )
            mDatabaseManager.actionAddPunch(punch)
            createNotificationForControlPoint(cp)
            Log.i(TAG, "added to list")
            false
        }
    }

    fun startOnClick() {
        mCnt = 0
        mPunches.clear()
        mBeaconManager.bind(this)

        createNotification()
        updateWakeLock()

        mDatabaseManager.actionStartEvent()
        mTrackingState = STATE_ON
    }

    fun stopOnClick() {
        if (mBeaconManager.isBound(this))
            mBeaconManager.unbind(this)


        updateWakeLock()
        mDatabaseManager.actionStopEvent()
        mDatabaseManager.actionGetPunchesByEventId(mDatabaseManager.actionGetLastEvent().id)
        mTrackingState = STATE_OFF

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
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                mWakeLock!!.apply {
                    acquire()
                }
                Log.i(TAG + "WAKELOCK", "ACQUIRE")
            }
        } else {
            mLastLockUpdateMillis = 0
            if (this.mWakeLock != null) {
                this.mWakeLock!!.release()
                this.mWakeLock = null
            }
            Log.i(TAG + "WAKELOCK", "RELEASED")

        }
    }
}
