package ru.hunkel.mgrouptracker.fragments


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_info.*
import kotlinx.android.synthetic.main.activity_info.view.*
import kotlinx.android.synthetic.main.event_list_item.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.database.entities.Event
import ru.hunkel.mgrouptracker.database.utils.DatabaseManager
import ru.hunkel.mgrouptracker.drawables.EventDrawable
import ru.hunkel.mgrouptracker.utils.PATTERN_FULL_DATE_INVERSE
import ru.hunkel.mgrouptracker.utils.convertMillisToTime

const val TAG = "EventsFragment"
const val ACTION_DELETE = 0

class EventsFragment : Fragment() {
    /*
        VARIABLES
    */
    private var events: MutableList<Event> = mutableListOf()

    private lateinit var mEventRecyclerView: RecyclerView
    private lateinit var mDatabaseManager: DatabaseManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mEventRecyclerView = event_recycler_view
        mEventRecyclerView.layoutManager = GridLayoutManager(context!!, 1)
        mDatabaseManager = DatabaseManager(context!!)

        events = mDatabaseManager.actionGetAllEvents().toMutableList()

        if (events.isEmpty()) {
            view.info_text_view.visibility = View.VISIBLE
        } else {
            view.info_text_view.visibility = View.GONE
        }

        events.sortByDescending {
            it.id
        }
        mEventRecyclerView.adapter = EventAdapter(events)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            ACTION_DELETE -> {
                val event = events[item.order]
                events.removeAt(item.order)
                mDatabaseManager.actionDeleteEvent(event)
                mEventRecyclerView.adapter!!.notifyItemRemoved(item.order)
//                mEventRecyclerView.adapter!!.notifyItemRangeChanged(item.order, events.size)
            }
        }
        return true
    }

    /*
        INNER CLASSES
    */
    private inner class EventAdapter(private val eventsList: List<Event>) :
        RecyclerView.Adapter<EventViewHolder>() {
        val colorList: IntArray = context!!.resources.getIntArray(R.array.background_item_colors)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.event_list_item, parent, false)
            return EventViewHolder(view)
        }

        override fun getItemCount(): Int {
            return eventsList.size
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val dr = EventDrawable(colorList[position % 2])
            holder.view.background = dr
            holder.bind(eventsList[position])
            holder.view.setOnClickListener {
                val action = EventsFragmentDirections.actionGoToPunchFragment()
                action.argumentEventId = eventsList[position].id
                findNavController().navigate(action)
            }
        }
    }

    private class EventViewHolder(val view: View) : RecyclerView.ViewHolder(view),
        View.OnCreateContextMenuListener {

        init {
            view.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu!!.setHeaderTitle("Выберите действие")
            menu.add(0, ACTION_DELETE, adapterPosition, "Удалить мероприятие")
        }

        fun bind(event: Event) {
            view.event_id_text_view.text = "Мероприятие №${event.id}"
            view.event_start_time_text_view.text =
                "Время начала: ${convertMillisToTime(event.startTime, PATTERN_FULL_DATE_INVERSE)}"
            view.event_end_time_text_view.text =
                "Время окончания: ${convertMillisToTime(event.endTime, PATTERN_FULL_DATE_INVERSE)}"
        }
    }
}
