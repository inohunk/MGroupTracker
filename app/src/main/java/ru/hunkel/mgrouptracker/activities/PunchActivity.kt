package ru.hunkel.mgrouptracker.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_punch.*
import kotlinx.android.synthetic.main.event_list_item.view.*
import kotlinx.android.synthetic.main.punch_list_item.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Punches
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.utils.convertLongToTime

const val KEY_EVENT_ID = "keyEvent"

class PunchActivity : AppCompatActivity() {

    private lateinit var mPunchRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_punch)

        mPunchRecyclerView = punch_recycler_view
        mPunchRecyclerView.layoutManager = LinearLayoutManager(this)
        val dbManager = DatabaseManager(this)

        val punchId = intent.getIntExtra(KEY_EVENT_ID,1)

        val punches = dbManager.actionGetPunchesByEventId(punchId)
        mPunchRecyclerView.adapter = PunchAdapter(punches)
    }

    private class PunchAdapter(private val punchList: List<Punches>) : RecyclerView.Adapter<PunchViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.punch_list_item,parent,false)
            return PunchViewHolder(view)
        }

        override fun getItemCount(): Int {
            return punchList.size
        }

        override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
            holder.bind(punchList[position])
        }
    }

    private class PunchViewHolder(val view: View) : RecyclerView.ViewHolder(view){
        fun bind(punch: Punches){
            view.punch_id_text_view.text = "Отметка на ${punch.controlPoint}"
            view.punch_time_text_view.text = "Время отметки ${convertLongToTime(punch.time)}"
        }
    }
}
