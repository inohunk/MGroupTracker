package ru.hunkel.mgrouptracker.fragments


import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
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
    private val CUT_TIME = 60 * 15L

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.fragment_result, null)
        builder.setView(view)

        val endTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(roundMilliseconds(event.endTime))
        val startTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(roundMilliseconds(event.startTime))
        val runningTime = endTimeSeconds - startTimeSeconds


        val pm = getDefaultSharedPreferences(context)
        val cutNumFirst = pm.getString("cutoff_point_first", "250")!!.toInt()
        val cutNumSecond = pm.getString("cutoff_point_second", "251")!!.toInt()
        val cutEnabled = pm.getBoolean("cutoff_enabled", false)

        if (cutEnabled and punches.isNotEmpty()
            and
            (checkForCutPoint(cutNumFirst, punches)
                    or
                    checkForCutPoint(cutNumSecond, punches))
        ) {
            val cutTimeFirst = calculateCutTime(cutNumFirst, punches)
            val cutTimeSecond = calculateCutTime(cutNumSecond, punches)

            var summaryCutTime = cutTimeFirst + cutTimeSecond

            if (summaryCutTime > CUT_TIME) {
                summaryCutTime = CUT_TIME
            }

            val timeOnDistance = if ((summaryCutTime >= 0) and (summaryCutTime <= CUT_TIME)) {
                runningTime - summaryCutTime
            } else {
                runningTime - CUT_TIME
            }

            val time = convertMillisToTime(
                TimeUnit.SECONDS.toMillis(timeOnDistance),
                PATTERN_HOUR_MINUTE_SECOND,
                TimeZone.getTimeZone("UTC-3")
            )
            val cutTime = convertMillisToTime(
                TimeUnit.SECONDS.toMillis(summaryCutTime),
                PATTERN_HOUR_MINUTE_SECOND,
                TimeZone.getTimeZone("UTC-3")
            )
            view.time_on_distance_text_view.text =
                "Время(с отсечкой)\n\t $time\n" +
                        "Время отсечки $cutTime"
        } else {
            val timeOnDistance = convertMillisToTime(
                runningTime * 1000,
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

    private fun calculateCutTime(cutNum: Int, punches: List<Punches>): Long {
        val minId = punches.indexOfFirst {
            it.controlPoint == cutNum
        }

        val maxId = punches.indexOfLast {
            it.controlPoint == cutNum
        }
        var endTimeCut = 0L
        var startTimeCut = 0L
        try {
            endTimeCut = TimeUnit.MILLISECONDS.toSeconds(roundMilliseconds(punches[maxId].time))
            startTimeCut = TimeUnit.MILLISECONDS.toSeconds(roundMilliseconds(punches[minId].time))
        } catch (ex: Exception) {
        }

        var cutTime = endTimeCut - startTimeCut
        if (cutTime > CUT_TIME) {
            cutTime = CUT_TIME
        }
        return cutTime
    }

}