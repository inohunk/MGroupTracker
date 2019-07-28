package ru.hunkel.mgrouptracker.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_punch.*
import kotlinx.android.synthetic.main.punch_list_item.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.utils.convertLongToTime
import java.text.SimpleDateFormat
import java.util.*
import ru.hunkel.mgrouptracker.utils.PATTERN_HMS_DATE

const val KEY_EVENT_ID = "keyEvent"

class PunchActivity : AppCompatActivity() {

    private lateinit var mPunchRecyclerView: RecyclerView

    companion object {
        var eventStartTime = -1L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punch)

        mPunchRecyclerView = punch_recycler_view
        mPunchRecyclerView.layoutManager = LinearLayoutManager(this)
        val dbManager = DatabaseManager(this)

        val eventId = intent.getIntExtra(KEY_EVENT_ID, 1)
        eventStartTime = dbManager.actionGetEventById(eventId).startTime
        val punches = dbManager.actionGetPunchesByEventId(eventId)
        mPunchRecyclerView.adapter = PunchAdapter(punches)
    }

    private inner class PunchAdapter(private val punchList: List<Punches>) : RecyclerView.Adapter<PunchViewHolder>() {
        val colorList: IntArray = baseContext.resources.getIntArray(R.array.background_punch_item_colors)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.punch_list_item, parent, false)
            return PunchViewHolder(view)
        }

        override fun getItemCount(): Int {
            return punchList.size
        }

        override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
            holder.view.setBackgroundColor(colorList[position%2])
            holder.bind(punchList[position],position)
        }
    }

    private class PunchViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(punch: Punches,position:Int) {
            if (eventStartTime != -1L) {
                view.punch_position_text_view.text = "${position+1}."
                view.punch_id_text_view.text = "${punch.controlPoint}"
                val format = SimpleDateFormat("HH:mm:ss")
                format.timeZone = TimeZone.getTimeZone("UTC")
                val diff = punch.time - eventStartTime
                view.punch_time_text_view.text = "${convertLongToTime(punch.time,PATTERN_HMS_DATE)}"
                view.from_start_time_text_view.text = "${format.format(Date(diff))}"
            }
        }
    }
}
