package ru.hunkel.mgrouptracker.services

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.RingtoneManager
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
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.altbeacon.beacon.*
import org.json.JSONArray
import org.json.JSONObject
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.activities.HostActivity
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.fragments.BROADCAST_ACTION
import ru.hunkel.mgrouptracker.fragments.BROADCAST_TYPE
import ru.hunkel.mgrouptracker.fragments.BROADCAST_TYPE_FIX_TIME
import ru.hunkel.mgrouptracker.managers.TimeManager
import ru.hunkel.mgrouptracker.network.DataSender
import ru.hunkel.mgrouptracker.receivers.NetworkStateReceiver
import ru.hunkel.mgrouptracker.utils.*
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
        private const val NOTIFICATION_CHANNEL_NAME = "TrackingNotificationsChannel"

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
    private lateinit var mNotificationManager: NotificationManager
    private var mDefaultNotificationRingtoneUri: Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    //Database
    private lateinit var mDatabaseManager: DatabaseManager

    //Base
    private var updateControlPointAfter = DEFAULT_CONTROL_POINT_UPDATE

    private var mTrackingState = STATE_OFF

    //Network
    private val mDataSender = DataSender(this)

    private var mServerUrl = ""

    private var mServerUrlReceived = false

    //Network state receiver
    private val mNetworkStateReceiver = NetworkStateReceiver()

    private var mNetworkStateReceiverRegistered = false

    //Time
    private lateinit var mTimeManager: TimeManager

    private var mTimeSynchronized = false

    //Collections
    private val mPunchesIdentifiers = LinkedList<Int>()
    private val mPunches = LinkedList<Punches>()

    private var mPunchUpdateState = PUNCH_UPDATE_STATE_ADD

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
                mServerUrlReceived = true

            } catch (ex: Exception) {
                mServerUrlReceived = false
            }
            Log.i(
                TAG_OGPSCENTER, ".\nogpscenter connected" +
                        "\ncomponent name: ${name.toString()}" +
                        "\nurl getted: $mServerUrlReceived"
            )
            stopOGPSCenterService()

        }
    }

    /*
        INNER CLASSES
    */
    inner class TrackingServiceImpl : ITrackingService.Stub() {
        override fun startEvent() {
            startOnClick()
        }

        override fun stopEvent(): Long {
            val endTime = mTimeManager.getTime()
            mDatabaseManager.actionStopEvent(endTime)
            stopOnClick()
            return endTime
        }

        override fun getState(): Int {
            return this@TrackingService.mTrackingState
        }
    }

    /*
        Override functions
    */
    override fun onCreate() {
        mTimeManager = TimeManager(this)
        TimeManager.sTimeChangedListener = this
        mDatabaseManager = DatabaseManager(this)
        mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        startOGPSCenterService()
    }

    override fun onBind(intent: Intent): IBinder {
        return TrackingServiceImpl()
    }

    override fun onBeaconServiceConnect() {
        mBeaconManager.removeAllRangeNotifiers()

        val pm = getDefaultSharedPreferences(this)

        val distance = pm.getString("beacon_distance", "4.5")!!.toFloat()
        val scanPeriod = (pm.getString("beacon_scan_period", "1")!!.toFloat() * 1000).toLong()
//        val betweenScanPeriod =
//            (pm.getString("beacon_between_scan_period", "0")!!.toFloat() * 1000).toLong()
        val betweenScanPeriod = 0L
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

    override fun onDestroy() {
        super.onDestroy()
        if (mBeaconManager != null && mBeaconManager.isBound(this)) {
            mBeaconManager.unbind(this)
        }
        mNotificationManager.cancelAll()
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

    override fun onTimeChanged(time: Long) {
        fixTimeInDatabase()
        mTimeSynchronized = true
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
        val intent = Intent(this, HostActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 0
        )

        mBuilder!!.setContentIntent(pendingIntent)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            )
            mNotificationManager.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channel.id)
        }

        mBeaconManager.enableForegroundServiceScanning(
            mBuilder!!.build(),
            NOTIFICATION_STATE_ID
        )
        val notification = mBuilder!!.build()
        notification!!.flags = Notification.FLAG_NO_CLEAR
        mNotificationManager.notify(NOTIFICATION_STATE_ID, notification)
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
        mBuilder!!.setVibrate(longArrayOf(1000, 1000, 1000))
        mBuilder!!.setContentText("$controlPoint")
        mBuilder!!.priority = NotificationCompat.PRIORITY_MAX

        if (mDefaultNotificationRingtoneUri != null) {
            mBuilder?.setSound(mDefaultNotificationRingtoneUri)
        }

        mBuilder!!.setDefaults(NotificationCompat.DEFAULT_ALL)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            )
            mNotificationManager.createNotificationChannel(channel)
            mBuilder!!.setChannelId(channel.id)
        }

        mNotificationManager.notify(NOTIFICATION_CONTROL_POINT_ID, mBuilder!!.build())

    }


    private fun checkInList(cp: Int) {
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

                val time = getTimeFromService()
                punch.time = time
                Log.i(
                    TAG_BEACON, "Beacon $cp time updated - ${convertMillisToTime(
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
            val time = getTimeFromService()
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
//        if (mServerUrlReceived and DataSender.isNetworkConnected(this)) { // for testing
        if (mTimeSynchronized and mServerUrlReceived and DataSender.isNetworkConnected(this)) {
            val jsonString = createJsonByPunchList()

            CoroutineScope(Dispatchers.Default).launch {
                //                    if (mDataSender.sendPunchesAsync(jsonString, "https://postman-echo.com/get").await()){
//                    if (mDataSender.sendPunchesAsync(jsonString, "http://192.168.43.150:2023/").await()){
                if (mDataSender.sendPunchesAsync(jsonString, mServerUrl).await()) {
                    Log.i(TAG_NETWORK, "POSTED")
                } else {
                    Log.i(TAG_NETWORK, "NOT POSTED")
                    //TODO write timer task
                }

            }
        } else if (DataSender.isNetworkConnected(this).not()) {
            if (!mNetworkStateReceiverRegistered) {
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
                json.put("uid", "${i.controlPoint}")
                json.put("name", "cp_${i.controlPoint}")
                json.put(
                    "time",
                    convertMillisToTime(
                        i.time,
                        PATTERN_YEAR_MONTH_DAY,
                        TimeZone.getTimeZone("UTC")
                    ) + "T" +
                            convertMillisToTime(
                                i.time,
                                PATTERN_HOUR_MINUTE_SECOND,
                                TimeZone.getTimeZone("UTC")
                            ) + "Z"
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

    private fun getTimeFromService() = mTimeManager.getTime()

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

        initBeaconManager()
        mBeaconManager.bind(this)

        createNotification()
        updateWakeLock()

        mDatabaseManager.actionStartEvent(mTimeManager.getTime())
        mTrackingState = STATE_ON
    }

    fun stopOnClick() {
        if (mBeaconManager.isBound(this))
            mBeaconManager.unbind(this)
        mTimeManager.stopGps()
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
                        "${convertMillisToTime(p.time, PATTERN_HOUR_MINUTE_SECOND)}\n==="
            )

            p.time += timeDelta
            Log.i(
                TAG_BEACON,
                "new punch time ${convertMillisToTime(p.time, PATTERN_HOUR_MINUTE_SECOND)}"
            )
            Log.i(
                TAG_BEACON, "PUNCH-TIM\n${p.id}\n" +
                        "${p.eventId}\n" +
                        "${convertMillisToTime(p.time, PATTERN_HOUR_MINUTE_SECOND)}\n"
            )
            mDatabaseManager.actionReplacePunchSimple(p)
        }
        val broadcastIntent = Intent(BROADCAST_ACTION)
        broadcastIntent.putExtra(BROADCAST_TYPE, BROADCAST_TYPE_FIX_TIME)
        sendBroadcast(broadcastIntent)
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
}
