package ru.hunkel.mgrouptracker.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_punch.*
import kotlinx.android.synthetic.main.activity_punch.view.*
import kotlinx.android.synthetic.main.punch_list_item.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.utils.PATTERN_HOUR_MINUTE_SECOND
import ru.hunkel.mgrouptracker.utils.convertMillisToTime
import java.text.SimpleDateFormat
import java.util.*


class PunchFragment : Fragment() {
    private lateinit var mPunchRecyclerView: RecyclerView

    companion object {
        var eventStartTime = -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_punch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mPunchRecyclerView = punch_recycler_view
        mPunchRecyclerView.layoutManager = LinearLayoutManager(context)
        val dbManager = DatabaseManager(context!!)

        val eventId = PunchFragmentArgs.fromBundle(arguments!!).argumentEventId
        val event = dbManager.actionGetEventById(eventId)
        eventStartTime = event.startTime
        view.start_time_text_view.text = convertMillisToTime(
            eventStartTime,
            PATTERN_HOUR_MINUTE_SECOND
        )
        view.end_time_text_view.text = convertMillisToTime(
            event.endTime,
            PATTERN_HOUR_MINUTE_SECOND
        )
        val punches = dbManager.actionGetPunchesByEventIdWithAscSorting(eventId)
        if (punches.isEmpty()){
            view.start_time_text_view.text = "Нет отметок"
            view.end_time_text_view.visibility = View.GONE
        }
        mPunchRecyclerView.adapter = PunchAdapter(punches)
        result_button.visibility = View.VISIBLE
        result_button.setOnClickListener {
            val fragment = ResultFragment(
                event,
                punches
            )
            fragment.show(parentFragmentManager, "missiles")
        }
    }

    private inner class PunchAdapter(private val punchList: List<Punches>) :
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
            holder.view.setBackgroundColor(colorList[position % 2])
            holder.bind(punchList[position], position)
        }
    }

    private class PunchViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(punch: Punches, position: Int) {
            if (eventStartTime != -1L) {
                view.punch_position_text_view.text = "${position + 1}."
                view.punch_id_text_view.text = "${punch.controlPoint}"
                val format = SimpleDateFormat("HH:mm:ss")
                format.timeZone = TimeZone.getTimeZone("UTC")
                val diff = punch.time - eventStartTime
                view.punch_time_text_view.text =
                    "${convertMillisToTime(punch.time, PATTERN_HOUR_MINUTE_SECOND)}"
                view.from_start_time_text_view.text = "${format.format(Date(diff))}"
            }
        }
    }
}
