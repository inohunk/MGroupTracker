package ru.hunkel.mgrouptracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
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
import org.json.JSONArray
import org.json.JSONObject
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.fragments.BROADCAST_ACTION
import ru.hunkel.mgrouptracker.fragments.MainFragment
import ru.hunkel.mgrouptracker.utils.*
import ru.hunkel.servicesipc.ILocationService
import ru.ogpscenter.ogpstracker.service.IGPSTrackerServiceRemote
import java.util.*
import kotlin.math.abs

class TrackingService : Service(), BeaconConsumer {

    /*
        VARIABLES
    */
    companion object {
        //TAG's
        private const val TAG = "TrackingService"
        private const val WAKE_LOCK_TAG = "PunchingApp:WakeLockTag"

        //Notifications
        private const val NOTIFICATION_CHANNEL_ID = "TrackingNotifications"
        private const val NOTIFICATION_STATE_ID = 1
        private const val NOTIFICATION_CONTROL_POINT_ID = 2

        private const val LOCK_UPDATE_INTERVAL: Long = DEFAULT_LOCK_UPDATE_INTERVAL
    }

    private var mWakeLock: PowerManager.WakeLock? = null
    private var mLastLockUpdateMillis: Long = 0

    private lateinit var mBeaconManager: BeaconManager
    private var mBuilder: NotificationCompat.Builder? = null

    private var mTrackingState = STATE_OFF

    private lateinit var mDatabaseManager: DatabaseManager

    private var updateControlPointAfter = DEFAULT_CONTROL_POINT_UPDATE

    //Collections
    private val mPunchesIdentifiers = LinkedList<Int>()
    private val mPunches = LinkedList<Punches>()

    private var mPunchUpdateState = PUNCH_UPDATE_STATE_ADD

    //Objects
    private lateinit var mTimeManager: TimeManagerNew


    //LOCATION SERVICE
    var locationService: ILocationService? = null
    var isServiceConnected = false

    private val locationServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            Log.i(TAG, "locationService disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            locationService = ILocationService.Stub.asInterface(service)
            isServiceConnected = true
            Log.i(
                TAG, "locationService connected" +
                        "\ncomponent name: ${name.toString()}"
            )
        }
    }

    //OGPSCenter service connection
    var mGpsService: IGPSTrackerServiceRemote? = null

    private val mOGPSCenterServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mGpsService = null
            Log.i(TAG, "ogpscenter service disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mGpsService = IGPSTrackerServiceRemote.Stub.asInterface(service)
            Log.i(
                TAG, "ogpscenter connected" +
                        "\ncomponent name: ${name.toString()}"
            )
        }
    }

    /*
        INNER CLASSES
    */
    inner class TrackingServiceImpl : ITrackingService.Stub() {
        override fun startEvent() {
            mDatabaseManager.actionStartEvent(mTimeManager.getTime())
            startOnClick()
        }

        override fun stopEvent() {
            mDatabaseManager.actionStopEvent(mTimeManager.getTime())
            stopOnClick()
        }

        override fun getState(): Int {
            return this@TrackingService.mTrackingState
        }
    }

    private inner class TimeManagerNew(context: Context) : TimeManager(context) {
        override fun onGpsTimeReceived() {
            fixTimeInDatabase(mTimeManager.getTime())
        }
    }

    /*
        Override functions
    */

    override fun onCreate() {
        mDatabaseManager = DatabaseManager(this)
        mTimeManager = TimeManagerNew(this)
        initBeaconManager()
        startOnClick()
        startLocationService()
    }

    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }

    override fun onDestroy() {
        stopOnClick()
        stopLocationService()
    }

    override fun onBeaconServiceConnect() {
        mBeaconManager.removeAllRangeNotifiers()

        val pm = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

        val distance = pm.getString("beacon_distance", "4.5")!!.toFloat()
        val scanPeriod = (pm.getString("beacon_scan_period", "1")!!.toFloat() * 1000).toLong()
        val betweenScanPeriod = (pm.getString("beacon_between_scan_period", "0")!!.toFloat() * 1000).toLong()
        mPunchUpdateState = (pm.getString("update_punch_params", "0")).toInt()
        updateControlPointAfter = (pm.getString("beacon_update_interval", "10")!!.toFloat() * 1000).toLong()

        mBeaconManager.foregroundScanPeriod = scanPeriod
        mBeaconManager.backgroundBetweenScanPeriod = betweenScanPeriod
        mBeaconManager.backgroundScanPeriod = scanPeriod
        mBeaconManager.foregroundBetweenScanPeriod = betweenScanPeriod
        mBeaconManager.applySettings()

        Log.i(TAG + "DISTANCE", distance.toString())
        mBeaconManager.addRangeNotifier { beacons, _ ->

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

    /*
        Functions
     */
    private fun initBeaconManager() {
        mBeaconManager = BeaconManager.getInstanceForApplication(this)
        mBeaconManager.backgroundMode = true
        mBeaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"))
        mBeaconManager.setEnableScheduledScanJobs(false)
    }

    private fun createNotification() {
        mBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        mBuilder!!.setSmallIcon(R.drawable.ic_main_icon)
        mBuilder!!.setContentTitle("Соревнование идет!")
        val intent = Intent(this, MainFragment::class.java)
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
            mBeaconManager.enableForegroundServiceScanning(mBuilder!!.build(), NOTIFICATION_STATE_ID)
        } else {
            notificationManager.notify(NOTIFICATION_STATE_ID, mBuilder!!.build())
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
        mBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        mBuilder!!.setSmallIcon(R.drawable.ic_control_point)
        mBuilder!!.setLargeIcon(getBitmap(this, R.drawable.ic_control_point))
        mBuilder!!.setContentTitle("Новый контрольный пункт!")
        mBuilder!!.setContentText("$controlPoint")
        mBuilder!!.setSound(Uri.parse("android.resource://ru.hunkel.mgrouptracker/" + R.raw.notification_sound))
        mBuilder!!.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(NOTIFICATION_CONTROL_POINT_ID, mBuilder!!.build())

    }


    private fun checkInList(cp: Int) {
        val newPunch = Punches(
            eventId = mDatabaseManager.actionGetLastEvent().id,
            time = mTimeManager.getTime(),
            controlPoint = cp
        )
        if (mPunchesIdentifiers.contains(cp)) {
            Log.i(TAG, "already exists in list")
            val punch = findPunchByControlPoint(cp)
            val currentTime = mTimeManager.getTime()

            if ((currentTime - punch.time > updateControlPointAfter) and (mPunchUpdateState != PUNCH_UPDATE_STATE_NOTHING)) {
                Log.i(TAG, "TIME FOR CONTROL POINT NEED TO BE UPDATED")

                val time = getTimeFromService(cp)
                punch.time = time
                mPunches.remove(punch)
                mPunches.add(newPunch)

                val broadcastIntent = Intent(BROADCAST_ACTION)

                when (mPunchUpdateState) {
                    PUNCH_UPDATE_STATE_ADD -> {
                        mDatabaseManager.actionAddPunch(newPunch)
                    }
                    PUNCH_UPDATE_STATE_REPLACE -> {
                        mDatabaseManager.actionReplacePunch(newPunch)
                    }
                }
                sendBroadcast(broadcastIntent)
                createNotificationForControlPoint(cp)
            }
        } else {
            val time = getTimeFromService(cp)
            newPunch.time = time
            mPunchesIdentifiers.add(cp)
            mPunches.add(newPunch)
            mDatabaseManager.actionAddPunch(newPunch)
            val intent = Intent(BROADCAST_ACTION)
            sendBroadcast(intent)
            createNotificationForControlPoint(cp)
            Log.i(TAG, "added to list")

            sendPunches()
        }
    }

    private fun sendPunches() {
        val jsonString = createJsonByPunchList()
        //TODO send to OGPSCenter
    }

    private fun createJsonByPunchList(): String {
        val list = mDatabaseManager.actionGetPunchesByEventId(mDatabaseManager.actionGetLastEvent().id)
        val jsonArray = JSONArray()

        for (i in list) {
            val json = JSONObject()
            json.put("uid", i.controlPoint)
            json.put("name", "КП ${i.controlPoint}")
            json.put("time", i.time)
            json.put("score", (i.controlPoint / 10))
            json.put("priority", 400)


            val coordinates = JSONObject()

            coordinates.put("latitude", 0F)
            coordinates.put("longitude", 0F)

            json.put("coordinates", coordinates)
            json.put("agent", "")
            json.put("comment", "")
            jsonArray.put(json)
        }
        Log.i(TAG, "JSON $jsonArray")
        return jsonArray.toString()
    }

    private fun getTimeFromService(cp: Int): Long {
        var time = -1L
        if (isServiceConnected) {
            time = locationService!!.punch(cp)
            Log.i(TAG + "REMOTE", convertLongToTime(time))
        } else {
            time = mTimeManager.getTime()
        }
        return time
    }

    private fun findPunchByControlPoint(cp: Int): Punches {
        var punch: Punches = Punches(0, 0, 0, 0)

        for (p in mPunches) {
            if (p.controlPoint == cp) {
                punch = p
            }
        }
        return punch
    }

    fun startOnClick() {
        mPunchesIdentifiers.clear()
        mBeaconManager.bind(this)

        createNotification()
        updateWakeLock()

        mDatabaseManager.actionStartEvent(mTimeManager.getTime())
        mTrackingState = STATE_ON
    }

    fun stopOnClick() {
        if (mBeaconManager.isBound(this))
            mBeaconManager.unbind(this)

        updateWakeLock()
        mDatabaseManager.actionStopEvent(mTimeManager.getTime())
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

    private fun fixTimeInDatabase(time: Long) {
        val punchList = mDatabaseManager.actionGetPunchesBeforeCertainTime(time)
        val event = mDatabaseManager.actionGetLastEvent()
        event.startTime += mTimeManager.getTimeDifference()
        mDatabaseManager.actionUpdateEvent(event)

        for (p in punchList) {
            Log.i(TAG, "PUNCH-TIME-SERVICE -------------------------")
            Log.i(TAG, "PUNCH-TIME-SERVICE ${p.time}")
            p.time += mTimeManager.getTimeDifference()
            Log.i(TAG, "PUNCH-TIME-SERVICE ${p.time}")
            mDatabaseManager.actionReplacePunchSimple(p)

            val broadcastIntent = Intent(BROADCAST_ACTION)
            sendBroadcast(broadcastIntent)
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.hunkel.servicesipc",
            "ru.hunkel.servicesipc.services.LocationService"
        )
        try {
            val res = bindService(
                serviceIntent,
                locationServiceConnection,
                Context.BIND_WAIVE_PRIORITY
            )
            Log.i(TAG + "TEST", "Service started: $res")
        } catch (ex: Exception) {
            Log.e(TAG, ex.message)
        }
    }

    private fun stopLocationService() {
        if (isServiceConnected) {
            locationService!!.stopTracking()
            unbindService(locationServiceConnection)
            isServiceConnected = false
        }
    }

    private fun startOGPSCenterService(){
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.ogpscenter.ogpstracker",
            "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
        )
        try {
            val res = bindService(
                serviceIntent,
                mOGPSCenterServiceConnection,
                Context.BIND_WAIVE_PRIORITY
            )
            Log.i(TAG + "TEST", "Service started: $res")
        } catch (ex: Exception) {
            Log.e(TAG, ex.message)
        }
    }

}
