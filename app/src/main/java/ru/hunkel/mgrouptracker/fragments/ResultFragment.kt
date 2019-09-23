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
    private val CUT_TIME = 60*15L

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
        if (cutEnabled and punches.isNotEmpty() and checkForCutPoint(cutNum, punches)) {
            val minId = punches.indexOfFirst {
                it.controlPoint == cutNum
            }

            val maxId = punches.indexOfLast {
                it.controlPoint == cutNum
            }
            var endTimeCut = 0L
            var startTimeCut = 0L
            try {
                endTimeCut = TimeUnit.MILLISECONDS.toSeconds(punches[maxId].time)
                startTimeCut = TimeUnit.MILLISECONDS.toSeconds(punches[minId].time)
            } catch (ex: Exception) {
            }

            val cutTime = endTimeCut - startTimeCut
            val timeOnDistance = if ((cutTime >= 0) and (cutTime <= CUT_TIME)) {
                TimeUnit.MILLISECONDS.toSeconds(runningTime) - CUT_TIME + (CUT_TIME - cutTime)
            } else {
                TimeUnit.MILLISECONDS.toSeconds(runningTime) - CUT_TIME
            }

            val time = convertMillisToTime(
                TimeUnit.SECONDS.toMillis(timeOnDistance),
                PATTERN_HOUR_MINUTE_SECOND,
                TimeZone.getTimeZone("UTC-3")
            )
            view.time_on_distance_text_view.text =
                "Время(с отсечкой)\n\t $time"
        } else {
            val timeOnDistance = convertMillisToTime(
                runningTime,
                PATTERN_HOUR_MINUTE_SECOND,
                TimeZone.getTimeZone("UTC-3")
            )
            view.time_on_distance_text_view.text =
                "Время(без отсечки)\n\t $timeOnDistance"
        }
        return builder.create()
    }

    private fun checkForCutPoint(num: Int, punches: List<Punches>): Boolean {
        val p = punches.find {
            it.controlPoint == num
        }
        return p != null
    }

}