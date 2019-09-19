package ru.hunkel.mgrouptracker.fragments


import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri.fromParts
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_punch.*
import kotlinx.android.synthetic.main.activity_punch.view.*
import kotlinx.android.synthetic.main.punch_list_item.view.*
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.drawables.EventDrawable
import ru.hunkel.mgrouptracker.services.TrackingService
import ru.hunkel.mgrouptracker.utils.PATTERN_HOUR_MINUTE_SECOND
import ru.hunkel.mgrouptracker.utils.convertLongToTime
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val BROADCAST_ACTION = "ru.hunkel.mgrouptracker.activities"
const val BROADCAST_TYPE_FIX_TIME = "Time fixing"

const val EXTRA_CONTROL_POINT = "broadcastControlPoint"

const val REQUEST_GPS = 1
const val REQUEST_BLUETOOTH = 2

class MainFragment : Fragment() {
    /*
            VARIABLES
        */
    companion object {
        var mServiceBounded = false
    }

    private var mTrackingService: ITrackingService? = null

    private lateinit var mDbManager: DatabaseManager

    private lateinit var mPunchRecyclerView: RecyclerView

    private lateinit var mBroadcastReceiver: BroadcastReceiver

    private var mGpsPermissionAccepted = false

    private val mTrackingServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mTrackingService = ITrackingService.Stub.asInterface(service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_main, container, false)
        view.start_time_text_view.visibility = View.GONE
        view.end_time_text_view.visibility = View.GONE
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        start_event_button.background =
            EventDrawable(ContextCompat.getColor(context!!, R.color.color_start_button))
        stop_event_button.background =
            EventDrawable(ContextCompat.getColor(context!!, R.color.color_stop_button))

        start_event_button.setOnClickListener {
            startServiceOnClick()
        }

        stop_event_button.setOnClickListener {
            stopServiceOnClick()
        }

        result_button.setOnClickListener {
            val fragment = ResultDialog(
                mDbManager.actionGetLastEvent(),
                mDbManager.actionGetPunchesByEventId(mDbManager.actionGetLastEvent().id)
            )
            fragment.show(fragmentManager!!,"missiles")
        }
        updateUIWithCurrentState(false)

        mPunchRecyclerView = punch_recycler_view
        mPunchRecyclerView.layoutManager = LinearLayoutManager(context!!)
        mPunchRecyclerView.adapter = PunchAdapter(mutableListOf())

        mDbManager = DatabaseManager(context!!)

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val list = mDbManager.actionGetPunchesByEventId(mDbManager.actionGetLastEvent().id)
                //TODO optimize list updating
                (mPunchRecyclerView.adapter!! as PunchAdapter).updateItems(list)
                when (intent!!.type) {
                    BROADCAST_TYPE_FIX_TIME -> {
                        start_time_text_view.text = convertLongToTime(
                            mDbManager.actionGetLastEvent().startTime,
                            PATTERN_HOUR_MINUTE_SECOND
                        )
                    }
                }
            }
        }
        context!!.registerReceiver(mBroadcastReceiver, IntentFilter(BROADCAST_ACTION))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.overflow_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_button -> {
                if (mServiceBounded) {
                    Toast.makeText(
                        context,
                        "Нельзя открыть настройки во время соревнования.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    findNavController().navigate(MainFragmentDirections.actionGoToSettingsActivity())
                }
            }
            R.id.info_button -> {
                if (mServiceBounded) {
                    Toast.makeText(
                        context,
                        "Нельзя открыть во время соревнования.",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    findNavController().navigate(MainFragmentDirections.actionGoToEventsFragment())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startServiceOnClick() {
        acceptPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ), REQUEST_GPS
        )
    }

    private fun stopServiceOnClick() {
        end_time_text_view.apply {
            text = convertLongToTime(
                mTrackingService!!.stopEvent(),
                PATTERN_HOUR_MINUTE_SECOND
            )
            visibility = View.VISIBLE
        }
        context!!.unbindService(mTrackingServiceConnection)
        context!!.stopService(Intent(context, TrackingService::class.java))
        updateUIWithCurrentState(false)
        mServiceBounded = false
    }

    private fun updateUIWithCurrentState(state: Boolean) {
        start_event_button.isEnabled = !state
        stop_event_button.isEnabled = state
    }

    override fun onDestroy() {
        if (mServiceBounded) {
            context!!.unbindService(mTrackingServiceConnection)
        }
        context!!.unregisterReceiver(mBroadcastReceiver)
        super.onDestroy()
    }

    private fun acceptPermissions(permissions: Array<String>, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        context!!,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(
                        context,
                        "Разрешение на местоположение необходимо для записи ваших треков",
                        Toast.LENGTH_LONG
                    ).show()
                    if ((shouldShowRequestPermissionRationale(permission))) {
                        requestPermissions(permissions, requestCode)
                    } else {
                        val intent = Intent()
                        intent.action = ACTION_APPLICATION_DETAILS_SETTINGS

                        val uri = fromParts("package", context!!.packageName, null)
                        intent.data = uri

                        startActivity(intent)
                    }

                } else {
                    mGpsPermissionAccepted = true
                    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                    if (mBluetoothAdapter.isEnabled) {
                        startTracking()
                    } else {
                        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        startActivityForResult(intent, REQUEST_BLUETOOTH)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_GPS -> {
                if (permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION)
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        mGpsPermissionAccepted = true
                        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                        if (mBluetoothAdapter.isEnabled && mGpsPermissionAccepted) {
                            startTracking()
                        } else {
                            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            startActivityForResult(intent, REQUEST_BLUETOOTH)
                        }
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_BLUETOOTH) {
            when (resultCode) {
                RESULT_OK -> {
                    if (mGpsPermissionAccepted) {
                        startTracking()
                    }
                }
                RESULT_CANCELED -> {
                    Toast.makeText(
                        context,
                        "Включите блютуз, чтобы можно было начать поиск маячков",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startTracking() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val serviceIntent =
                Intent(context, TrackingService::class.java)
            context!!.apply {
                startService(serviceIntent)
                bindService(
                    serviceIntent,
                    mTrackingServiceConnection,
                    Context.BIND_WAIVE_PRIORITY
                )
            }
            start_time_text_view.text = convertLongToTime(
                System.currentTimeMillis(),
                PATTERN_HOUR_MINUTE_SECOND
            )
            start_time_text_view.visibility = View.VISIBLE
            end_time_text_view.visibility = View.GONE
            updateUIWithCurrentState(true)
            mServiceBounded = true
            try {
                (mPunchRecyclerView.adapter as PunchAdapter).clear()
            } catch (ex: Exception) {
                Log.e(TAG, ex.message)
            }
        }
    }

    /*
        INNER CLASSES
    */
    private inner class PunchAdapter(private val punchList: MutableList<Punches>) :
        RecyclerView.Adapter<PunchViewHolder>() {
        val colorList: IntArray =
            context!!.resources.getIntArray(R.array.background_punch_item_colors)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.punch_list_item, parent, false)
            return PunchViewHolder(view)
        }

        override fun getItemCount(): Int {
            return punchList.size
        }

        override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
            holder.view.setBackgroundColor(colorList[(position % 2)])
            holder.bind(punchList[position], position)
        }

        fun addItem(punch: Punches) {
            punchList.add(punch)
            notifyDataSetChanged()
        }

        fun replaceItem(punch: Punches) {
            punchList.forEach {
                if (punch.controlPoint == it.controlPoint) {
                    it.time = punch.time
                }
            }
            notifyDataSetChanged()
        }

        fun updateItems(items: List<Punches>) {
            punchList.clear()
            punchList.addAll(0, items)
            notifyDataSetChanged()
        }

        fun clear() {
            punchList.clear()
            mPunchRecyclerView.adapter!!.notifyDataSetChanged()
        }
    }

    private inner class PunchViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(punch: Punches, position: Int) {
            view.punch_position_text_view.text = "${position + 1}."
            view.punch_id_text_view.text = "${punch.controlPoint}"

            val eventStartTime = mDbManager.actionGetLastEvent().startTime
            val format = SimpleDateFormat("HH:mm:ss")
            format.timeZone = TimeZone.getTimeZone("UTC")
            val diff = abs(punch.time - eventStartTime)
            view.punch_time_text_view.text =
                "${convertLongToTime(punch.time, PATTERN_HOUR_MINUTE_SECOND)}"
            view.from_start_time_text_view.text = "${format.format(Date(diff))}"
        }
    }
}
