package ru.hunkel.mgrouptracker.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.event_results.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.utils.PATTERN_HOUR_MINUTE_SECOND
import ru.hunkel.mgrouptracker.utils.convertLongToTime
import java.util.*

class ResultDialog(
    private val event: Event,
    private val punches: List<Punches>
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = activity!!.layoutInflater
        var view = inflater.inflate(R.layout.event_results, null)
        builder.setView(view)

        val timeOnDistance = convertLongToTime(
            (event.endTime - event.startTime),
            PATTERN_HOUR_MINUTE_SECOND,
            TimeZone.getTimeZone("UTC-3")
        )

        view.time_on_distance_text_view.text = "Время: $timeOnDistance"

        view.control_points_text_view.text = "КП: " + punches.size.toString()
        return builder.create()
    }

}