package ru.hunkel.mgrouptracker.fragments


import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_result.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.utils.PATTERN_HOUR_MINUTE_SECOND
import ru.hunkel.mgrouptracker.utils.convertLongToTime
import java.util.*

class ResultFragment(
    private val event: Event,
    private val punches: List<Punches>
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        var view = inflater.inflate(R.layout.fragment_result, null)
        builder.setView(view)

        val timeOnDistance = convertLongToTime(
            (event.endTime - event.startTime),
            PATTERN_HOUR_MINUTE_SECOND,
            TimeZone.getTimeZone("UTC-3")
        )

        view.time_on_distance_text_view.text = "Время: $timeOnDistance"

        return builder.create()
    }

}