package ru.hunkel.mgrouptracker.fragments


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_punch.*
import kotlinx.android.synthetic.main.punch_list_item.view.*
import ru.hunkel.mgrouptracker.ITrackingService
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.activities.TrackingSettings
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.drawables.EventDrawable
import ru.hunkel.mgrouptracker.services.TrackingService
import ru.hunkel.mgrouptracker.utils.PATTERN_HMS_DATE
import ru.hunkel.mgrouptracker.utils.convertLongToTime
import ru.hunkel.mgrouptracker.utils.enableBluetooth
import java.text.SimpleDateFormat
import java.util.*

const val BROADCAST_ACTION = "ru.hunkel.mgrouptracker.activities"
const val EXTRA_CONTROL_POINT = "broadcastControlPoint"
const val TAG = "MainFragment"

/**
 * A simple [Fragment] subclass.
 *
 */
class MainFragment : Fragment() {
    /*
            VARIABLES
        */
    var mTrackingService: ITrackingService? = null
    private var mServiceBounded = false

    lateinit var mDbManager: DatabaseManager

    private lateinit var mPunchRecyclerView: RecyclerView

    private lateinit var mBroadcastReceiver: BroadcastReceiver

    private val mTrackingServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {

        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mTrackingService = ITrackingService.Stub.asInterface(service)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        start_event_button.background = EventDrawable(ContextCompat.getColor(context!!, R.color.color_start_button))
        stop_event_button.background = EventDrawable(ContextCompat.getColor(context!!, R.color.color_stop_button))

        start_event_button.setOnClickListener {
            startServiceOnClick()
        }

        stop_event_button.setOnClickListener {
            stopServiceOnClick()
        }
        updateUIWithCurrentState(false)

        mPunchRecyclerView = punch_recycler_view
        mPunchRecyclerView.layoutManager = LinearLayoutManager(context)
        mDbManager = DatabaseManager(context!!)

        try {
            mPunchRecyclerView.adapter = PunchAdapter(mutableListOf())
        } catch (ex: Exception) {
            Log.e(TAG, ex.message)
        }

        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val list = mDbManager.actionGetPunchesByEventId(mDbManager.actionGetLastEvent().id)
                (mPunchRecyclerView.adapter!! as PunchAdapter).updateItems(list)
            }
        }

        context!!.registerReceiver(mBroadcastReceiver, IntentFilter(BROADCAST_ACTION))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.overflow_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item?.itemId) {
            R.id.settings_button -> {
                if (mServiceBounded.not()) {
                    startActivity(Intent(context, TrackingSettings::class.java))
                } else {
                    Toast.makeText(context, "Нельзя открыть настройки во время соревнования.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            R.id.info_button -> {
                findNavController().navigate(MainFragmentDirections.actionGoToEventsFragment())
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun startServiceOnClick() {
        if (acceptPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        ) {
            enableBluetooth()

            val serviceIntent =
                Intent(context, TrackingService::class.java)
            context!!.startService(serviceIntent)
            context!!.bindService(serviceIntent, mTrackingServiceConnection, Context.BIND_WAIVE_PRIORITY)

            updateUIWithCurrentState(true)
            mServiceBounded = true
            try {
                (mPunchRecyclerView.adapter as PunchAdapter).clear()
            } catch (ex: Exception) {
                Log.e(TAG, ex.message)
            }

        }
    }

    private fun stopServiceOnClick() {

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
        super.onDestroy()
        if (mServiceBounded) {
            context!!.unbindService(mTrackingServiceConnection)
        }
        context!!.unregisterReceiver(mBroadcastReceiver)
    }

    private fun acceptPermissions(permissions: Array<String>): Boolean {
        var grandted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (
                    (context!!.checkSelfPermission((permission)) != PackageManager.PERMISSION_GRANTED)
                    and
                    (shouldShowRequestPermissionRationale(permission))
                ) {
                    Toast.makeText(
                        context,
                        "Разрешение на местоположение необходимо для записи ваших треков",
                        Toast.LENGTH_LONG
                    ).show()
                    grandted = false
                }
            }
            requestPermissions(permissions, 1)
        } else {
            grandted = true
        }
        return grandted
    }

    /*
        INNER CLASSES
    */
    private inner class PunchAdapter(private val punchList: MutableList<Punches>) :
        RecyclerView.Adapter<PunchViewHolder>() {
        val colorList: IntArray = context!!.resources.getIntArray(R.array.background_punch_item_colors)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.punch_list_item, parent, false)
            return PunchViewHolder(view)
        }

        override fun getItemCount(): Int {
            return punchList.size
        }

        override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
            holder.view.setBackgroundColor(colorList[position % 2])
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
            val diff = punch.time - eventStartTime
            view.punch_time_text_view.text = "${convertLongToTime(punch.time, PATTERN_HMS_DATE)}"
            view.from_start_time_text_view.text = "${format.format(Date(diff))}"
        }
    }
}
