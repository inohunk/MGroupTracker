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
import ru.hunkel.mgrouptracker.utils.convertMillisToTime
import ru.hunkel.mgrouptracker.utils.roundMilliseconds
import java.util.*
import java.util.concurrent.TimeUnit

class ResultFragment(
    private val event: Event,
    private val punches: List<Punches>
) : DialogFragment() {
    private val CUT_TIME = 15L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        var view = inflater.inflate(R.layout.fragment_result, null)
        builder.setView(view)

        val endTimeNanos = roundMilliseconds(event.endTime)
        val startTimeNanos = roundMilliseconds(event.startTime)
        val runningTime =
            roundMilliseconds(
                endTimeNanos - startTimeNanos
            )

        val pm = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val cutNum = pm.getString("cutoff_number", "-1")!!.toInt()
        val cutEnabled = pm.getBoolean("cutoff_enabled", false)
        if ((cutNum == -1) and (cutEnabled.not())) {
            val timeOnDistance = convertMillisToTime(
                runningTime,
                PATTERN_HOUR_MINUTE_SECOND,
                TimeZone.getTimeZone("UTC-3")
            )
            view.time_on_distance_text_view.text =
                "Время(отсечка не установлена)\n\t $timeOnDistance"
        } else {
            val minId = punches.indexOfFirst {
                it.controlPoint == cutNum
            }

            val maxId = punches.indexOfLast {
                it.controlPoint == cutNum
            }
            val endTimeCutNanos = TimeUnit.MILLISECONDS.toSeconds(punches[maxId].time)
            val startTimeCutNanos = TimeUnit.MILLISECONDS.toSeconds(punches[minId].time)
            val deltaFTime = endTimeCutNanos - startTimeCutNanos

            if ((deltaFTime >= 0) and (deltaFTime <= CUT_TIME)) {
                var timeOnDistance = TimeUnit.MILLISECONDS.toSeconds(runningTime) - CUT_TIME + (CUT_TIME - deltaFTime)
//                timeOnDistance = roundMilliseconds(timeOnDistance)
                val time = convertMillisToTime(
                    TimeUnit.SECONDS.toMillis(timeOnDistance),
                    PATTERN_HOUR_MINUTE_SECOND,
                    TimeZone.getTimeZone("UTC-3")
                )
                view.time_on_distance_text_view.text =
                    "Время(отсечка установлена)\n\t $time"
            }
        }


        return builder.create()
    }

}