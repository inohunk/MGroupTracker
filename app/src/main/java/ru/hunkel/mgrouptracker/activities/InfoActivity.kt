package ru.hunkel.mgrouptracker.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_info.*
import kotlinx.android.synthetic.main.event_list_item.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager

class InfoActivity : AppCompatActivity() {

    private lateinit var mEventRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        mEventRecyclerView = event_recycler_view
        mEventRecyclerView.layoutManager = LinearLayoutManager(this)
        val dbManager = DatabaseManager(this)

        val events = dbManager.actionGetAllEvents()
        mEventRecyclerView.adapter = EventAdapter(events)
    }

    private class EventAdapter(private val eventsList: List<Event>) : RecyclerView.Adapter<EventViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.event_list_item,parent,false)
            return EventViewHolder(view)
        }

        override fun getItemCount(): Int {
            return eventsList.size
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            holder.bind(eventsList[position])
        }
    }

    private class EventViewHolder(val view:View) : RecyclerView.ViewHolder(view){
        fun bind(event: Event){
            view.event_id_text_view.text = event.id.toString()
        }
    }
}
