package ru.hunkel.mgrouptracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.RemoteException
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import org.json.JSONArray
import org.json.JSONObject
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.fragments.BROADCAST_ACTION
import ru.hunkel.mgrouptracker.fragments.BROADCAST_TYPE
import ru.hunkel.mgrouptracker.fragments.BROADCAST_TYPE_FIX_TIME
import ru.hunkel.mgrouptracker.fragments.MainFragment
import ru.hunkel.mgrouptracker.managers.TimeManager
import ru.hunkel.mgrouptracker.network.DataSender
import ru.hunkel.mgrouptracker.receivers.NetworkStateReceiver
import ru.hunkel.mgrouptracker.utils.*
import ru.hunkel.servicesipc.ILocationService
import ru.ogpscenter.ogpstracker.service.IGPSTrackerServiceRemote
import java.util.*
import kotlin.math.abs

const val TAG_OGPSCENTER = "OGPSCenter"
const val TAG_NETWORK = "Network"
const val TAG_BEACON = "Beacon"

class TrackingService : Service(), BeaconConsumer,
    NetworkStateReceiver.NetworkStateReceiverListener,
    TimeManager.TimeChangedListener {

    /*
        VARIABLES
    */
    companion object {
        private const val TAG = "TrackingService"
        private const val WAKE_LOCK_TAG = "PunchingApp:WakeLockTag"

        //Notifications
        private const val NOTIFICATION_CHANNEL_ID = "TrackingNotifications"
        private const val NOTIFICATION_STATE_ID = 1
        private const val NOTIFICATION_CONTROL_POINT_ID = 2

        private const val LOCK_UPDATE_INTERVAL: Long = DEFAULT_LOCK_UPDATE_INTERVAL
    }

    //Wakelock
    private var mWakeLock: PowerManager.WakeLock? = null
    private var mLastLockUpdateMillis: Long = 0

    //Beacon
    private lateinit var mBeaconManager: BeaconManager

    //Notifications
    private var mBuilder: NotificationCompat.Builder? = null


    //Database
    private lateinit var mDatabaseManager: DatabaseManager

    //Base
    private var updateControlPointAfter = DEFAULT_CONTROL_POINT_UPDATE

    private var mTrackingState = STATE_OFF

    private var mServerUrl = ""

    private var mServerUrlGetted = false

    private var mTimeSynchronized = false

    //Collections
    private val mPunchesIdentifiers = LinkedList<Int>()
    private val mPunches = LinkedList<Punches>()

    private var mPunchUpdateState = PUNCH_UPDATE_STATE_ADD

    //Objects
    private lateinit var mTimeManager: TimeManager
    private val mDataSender = DataSender()

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

            try {
                mServerUrl = mGpsService!!.punchesUploadUrl
                Log.i(TAG_OGPSCENTER, "ogps url: " + mGpsService!!.punchesUploadUrl)
                mServerUrlGetted = true

            } catch (ex: Exception) {
                mServerUrlGetted = false
            }
            Log.i(
                TAG_OGPSCENTER, ".\nogpscenter connected" +
                        "\ncomponent name: ${name.toString()}" +
                        "\nurl getted: $mServerUrlGetted"
            )
            stopOGPSCenterService()

        }
    }

    //Network state receiver
    private val mNetworkStateReceiver = NetworkStateReceiver()
    private var mNetworkStateReceiverRegistered = false

    private lateinit var mConnectivityManager: ConnectivityManager

    /*
        INNER CLASSES
    */
    inner class TrackingServiceImpl : ITrackingService.Stub() {
        override fun startEvent() {
            mDatabaseManager.actionStartEvent(mTimeManager.getTime())
            startOnClick()
        }

        override fun stopEvent(): Long {
            val endTime = mTimeManager.getTime()
            mDatabaseManager.actionStopEvent(endTime)
            stopOnClick()
            stopLocationService()
            return endTime
        }

        override fun getState(): Int {
            return this@TrackingService.mTrackingState
        }
    }

    override fun onTimeChanged(time: Long) {
        fixTimeInDatabase()
        mTimeSynchronized = true
    }
    /*
        Override functions
    */

    override fun onCreate() {
        mTimeManager = TimeManager(this)
        TimeManager.sTimeChangedListener = this
        mDatabaseManager = DatabaseManager(this)

        initBeaconManager()
        startOnClick()
        startLocationService()
        startOGPSCenterService()

        mConnectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    }

    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }

    override fun onBeaconServiceConnect() {
        mBeaconManager.removeAllRangeNotifiers()

        val pm = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)

        val distance = pm.getString("beacon_distance", "4.5")!!.toFloat()
        val scanPeriod = (pm.getString("beacon_scan_period", "1")!!.toFloat() * 1000).toLong()
        val betweenScanPeriod =
            (pm.getString("beacon_between_scan_period", "0")!!.toFloat() * 1000).toLong()
        mPunchUpdateState =
            (pm.getString("update_punch_params", "0"))!!.toInt()
        updateControlPointAfter =
            (pm.getString("beacon_update_interval", "10")!!.toFloat() * 1000).toLong()

        mBeaconManager.foregroundScanPeriod = scanPeriod
        mBeaconManager.backgroundBetweenScanPeriod = betweenScanPeriod
        mBeaconManager.backgroundScanPeriod = scanPeriod
        mBeaconManager.foregroundBetweenScanPeriod = betweenScanPeriod
        mBeaconManager.applySettings()

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
                        TAG_BEACON, "FOUNDED BEACON:\n" +
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
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Controls Notification Channel",
                "Controls Notification", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Controls Notification Channel"
            notificationManager.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channel.id)
        }

        mBeaconManager.enableForegroundServiceScanning(
            mBuilder!!.build(),
            NOTIFICATION_STATE_ID
        )
        notificationManager.notify(NOTIFICATION_STATE_ID, mBuilder!!.build())
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
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "Control Points Notification Channel",
                "Control Points Notification", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channel.id)
        }


        notificationManager.notify(NOTIFICATION_STATE_ID, mBuilder!!.build())

        notificationManager.notify(NOTIFICATION_CONTROL_POINT_ID, mBuilder!!.build())

    }


    private fun checkInList(cp: Int) {
        //TODO if checking fix
        val newPunch = Punches(
            eventId = mDatabaseManager.actionGetLastEvent().id,
            time = mTimeManager.getTime(),
            controlPoint = cp
        )
        if (mPunchesIdentifiers.contains(cp)) {
            Log.i(TAG_BEACON, "Beacon $cp already exists in list")
            val punch = findPunchByControlPoint(cp)
            val currentTime = mTimeManager.getTime()

            if ((currentTime - punch.time > updateControlPointAfter) and (mPunchUpdateState != PUNCH_UPDATE_STATE_NOTHING)) {

                val time = getTimeFromService(cp)
                punch.time = time
                Log.i(
                    TAG_BEACON, "Beacon $cp time updated - ${convertLongToTime(
                        time,
                        PATTERN_FULL_DATE_INVERSE
                    )}"
                )
                mPunches.remove(punch)
                mPunches.add(newPunch)

                val broadcastIntent = Intent(BROADCAST_ACTION)

                when (mPunchUpdateState) {
                    PUNCH_UPDATE_STATE_ADD -> {
                        mDatabaseManager.actionAddPunch(newPunch)
                    }
                    PUNCH_UPDATE_STATE_REPLACE -> {
                        mDatabaseManager.actionReplacePunchSimple(newPunch)
                    }
                }
                sendPunches()
                sendBroadcast(broadcastIntent)
                createNotificationForControlPoint(cp)
            } else {
                Log.i(TAG, "else")
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
            Log.i(TAG_BEACON, "Beacon $cp added to list")
            sendPunches()
        }
    }

    private fun sendPunches() {
        //TODO check server url for availability
        if (isConnected() and mTimeSynchronized and mServerUrlGetted) {
            val jsonString = createJsonByPunchList()

            if (mServerUrl != "") {
                CoroutineScope(Dispatchers.Default).launch {
                    //        mDataSender.sendPunches(jsonString, "http://192.168.43.150:2023/")
//                    mDataSender.sendPunches(jsonString, "https://postman-echo.com/post"){
//                    if (mDataSender.sendPunchesAsync(jsonString, "https://postman-echo.com/get").await()){
                    if (mDataSender.sendPunchesAsync(jsonString, mServerUrl).await()) {
                        Log.i(TAG_NETWORK, "POSTED")
                    } else {
                        Log.i(TAG_NETWORK, "NOT POSTED")
                        //TODO write timer task
                    }
                }
            }
        } else if (isConnected().not()) {
            if (!mNetworkStateReceiverRegistered) {
                //TODO rewrite with network callback
                NetworkStateReceiver.networkStateReceiverListener = this
                registerReceiver(
                    mNetworkStateReceiver,
                    IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                )
                mNetworkStateReceiverRegistered = true
                Log.i(
                    TAG_NETWORK,
                    "Network Broadcast Receiver: NO NETWORK CONNECTION. RECEIVER REGISTERED"
                )
            }
        } else if (mTimeSynchronized.not()) {
            //TODO do some if time not sync yet
        }
    }

    private fun createJsonByPunchList(): String {
        val list =
            mDatabaseManager.actionGetPunchesByEventId(mDatabaseManager.actionGetLastEvent().id)
        val jsonArray = JSONArray()
        val punchList = mutableListOf<Int>()

        for (i in list) {
            if (punchList.contains(i.controlPoint)) {
                continue
            } else {
                punchList.add(i.controlPoint)
                val json = JSONObject()
                json.put("uid", i.controlPoint.toString())
                json.put("name", "КП ${i.controlPoint}")
                json.put(
                    "time",
                    convertLongToTime(i.time, PATTERN_YEAR_MONTH_DAY) + "T" +
                            convertLongToTime(i.time, PATTERN_HOUR_MINUTE_SECOND) + "Z"
                )
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
        }
        Log.i(TAG, "JSON $jsonArray")
        return jsonArray.toString()
    }

    private fun getTimeFromService(cp: Int): Long {
        return if (isServiceConnected) {
            locationService!!.punch(cp)
        } else {
            mTimeManager.getTime()
        }
    }

    private fun findPunchByControlPoint(cp: Int): Punches {
        var punch = Punches(0, 0, 0, 0)

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

    private fun fixTimeInDatabase() {
        val event = mDatabaseManager.actionGetLastEvent()
        val punchList = mDatabaseManager.actionGetPunchesByEventId(event.id)
        val timeDelta = mTimeManager.getTimeDelta()
        event.startTime += timeDelta
        mDatabaseManager.actionUpdateEvent(event)

        for (p in punchList) {
            Log.i(TAG_BEACON, "PUNCH-TIME-SERVICE -------------------------")
            Log.i(
                TAG_BEACON, "PUNCH-TIM\n${p.id}\n" +
                        "${p.eventId}\n" +
                        "${convertLongToTime(p.time, PATTERN_HOUR_MINUTE_SECOND)}\n==="
            )

            p.time += timeDelta
            Log.i(
                TAG_BEACON,
                "new punch time ${convertLongToTime(p.time, PATTERN_HOUR_MINUTE_SECOND)}"
            )
            Log.i(
                TAG_BEACON, "PUNCH-TIM\n${p.id}\n" +
                        "${p.eventId}\n" +
                        "${convertLongToTime(p.time, PATTERN_HOUR_MINUTE_SECOND)}\n"
            )
            mDatabaseManager.actionReplacePunchSimple(p)
        }
        val broadcastIntent = Intent(BROADCAST_ACTION)
        broadcastIntent.putExtra(BROADCAST_TYPE, BROADCAST_TYPE_FIX_TIME)
        sendBroadcast(broadcastIntent)
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
            Log.i(TAG, "Service started: $res")
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

    private fun startOGPSCenterService() {
        val serviceIntent = Intent()
        serviceIntent.component = ComponentName(
            "ru.ogpscenter.ogpstracker",
            "ru.ogpscenter.ogpstracker.service.GPSTrackerService"
        )
        try {
            val res = bindService(
                serviceIntent,
                mOGPSCenterServiceConnection,
                Context.BIND_AUTO_CREATE
            )
            Log.i(TAG_OGPSCENTER, "Service started: $res")
        } catch (ex: Exception) {
            Log.e(TAG_OGPSCENTER, ex.message)
        }
    }

    private fun stopOGPSCenterService() {
        unbindService(mOGPSCenterServiceConnection)
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            sendPunches()
            Log.i(
                TAG_NETWORK,
                "Network Broadcast Receiver: CONNECTION ESTABLISHED. TRYING TO SEND AGAIN."
            )
        }
    }

    private fun isConnected(): Boolean {
        val networkInfo = mConnectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}
