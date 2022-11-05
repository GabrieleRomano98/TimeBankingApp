package com.example.timebankingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.Navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

data class Timeslot(
    var title: String,
    var description: String,
    var date: String,
    var place: String,
    var time: String,
    var hours: Int,
    var category: String,
    var id: String
)

class TimeSlotAdapter(
    private var timeslotList: MutableList<Timeslot>,
    private val myTimeslot: Boolean,
    private val onClick: (Timeslot) -> Unit,
    private val onDelete: (Timeslot) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder>() {

    fun setTimeslotList(newTimeslots: MutableList<Timeslot>) {
        val diffs = DiffUtil.calculateDiff(
            TimeslotDiffCallback(timeslotList, newTimeslots)
        )
        timeslotList = newTimeslots
        diffs.dispatchUpdatesTo(this)
    }

    class TimeSlotViewHolder(private val view: View) :
        RecyclerView.ViewHolder(view) {
        private val jobName: TextView = view.findViewById(R.id.list_item_job_name)
        private val jobHours: TextView = view.findViewById(R.id.list_item_job_hours)
        private val jobData: TextView = view.findViewById(R.id.list_item_job_time)
        private val jobLocation: TextView = view.findViewById(R.id.list_item_job_place)
        private val jobIcon: ImageView = view.findViewById(R.id.list_item_job_icon)
        private val jobBG: ConstraintLayout = view.findViewById(R.id.card_bg)
        private val jobEditBtn: ShapeableImageView = view.findViewById(R.id.editItem_Btn)
        private val jobDeleteBtn: ShapeableImageView = view.findViewById(R.id.deleteItem_Btn)
        private var currentTimeSlot: Timeslot? = null

        fun bind(
            timeslot: Timeslot,
            myTimeslot: Boolean,
            showDetails: (Timeslot) -> Unit,
            delete: (v: View) -> Unit
        ) {
            val hours = "${timeslot.hours} Hours"

            currentTimeSlot = timeslot
            jobName.text = timeslot.title
            jobHours.text = hours
            jobData.text = timeslot.date
            jobLocation.text = timeslot.place

            when (timeslot.category) {
                "Caregiving" -> {
                    jobBG.setBackgroundResource(R.drawable.pink_grad)
                    jobIcon.setImageResource(R.drawable.caregiving_drawable)
                }
                "Gardening" -> {
                    jobBG.setBackgroundResource(R.drawable.green_grad)
                    jobIcon.setImageResource(R.drawable.gardening_drawable2)
                }
                "Pet caring" -> {
                    jobBG.setBackgroundResource(R.drawable.orange_grad)
                    jobIcon.setImageResource(R.drawable.petcaring_drawable2)
                }
                "Housework" -> {
                    jobBG.setBackgroundResource(R.drawable.sky_grad)
                    jobIcon.setImageResource(R.drawable.housework_drawable)
                }
                "Handwork" -> {
                    jobBG.setBackgroundResource(R.drawable.yellow_grad)
                    jobIcon.setImageResource(R.drawable.handwork_drawable)
                }
                "Technology" -> {
                    jobBG.setBackgroundResource(R.drawable.blue_grad)
                    jobIcon.setImageResource(R.drawable.technology_drawable)
                }
                else -> {
                    jobBG.setBackgroundResource(R.drawable.grey_grad)
                    jobIcon.setImageResource(R.drawable.othercategory_drawable)
                }
            }

            view.setOnClickListener { currentTimeSlot?.let { showDetails(currentTimeSlot!!) } }

            if (!myTimeslot) {
                jobEditBtn.visibility = View.GONE
                jobDeleteBtn.visibility = View.GONE

            } else {
                jobDeleteBtn.setOnClickListener(delete)
                jobEditBtn.setOnClickListener {
                    val bundle = bundleOf("id" to currentTimeSlot!!.id)
                    findNavController(it).navigate(
                        R.id.action_timeslotList_to_myTimeslotDetails_Edit,
                        bundle
                    )
                }
            }

        }

        fun unbind(view: View, myTimeslot: Boolean) {
            view.setOnClickListener(null)
            if (myTimeslot) {
                jobEditBtn.setOnClickListener(null)
                jobDeleteBtn.setOnClickListener(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_timeslot_list_item, parent, false)
        return TimeSlotViewHolder(vg)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        val timeslot = timeslotList[position]
        holder.bind(timeslot, myTimeslot, {
            onClick(timeslot)
        }, {
            val pos = timeslotList.indexOf(timeslot)
            if (pos != -1) {
                timeslotList.removeAt(pos)
                notifyItemRemoved(pos)
                onDelete(timeslot)
            }
        }
        )
    }

    override fun onViewRecycled(holder: TimeSlotViewHolder) {
        holder.unbind(holder.itemView,myTimeslot)
    }

    override fun getItemCount(): Int = timeslotList.size

}

class TimeslotDiffCallback(private val ts: List<Timeslot>, private val newTs: List<Timeslot>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = ts.size
    override fun getNewListSize(): Int = newTs.size
    override fun areItemsTheSame(oldP: Int, newP: Int): Boolean {
        return (
                ts[oldP].id == newTs[newP].id &&
                        ts[oldP].title == newTs[newP].title &&
                        ts[oldP].date == newTs[newP].date &&
                        ts[oldP].time == newTs[newP].time &&
                        ts[oldP].place == newTs[newP].place &&
                        ts[oldP].hours == newTs[newP].hours &&
                        ts[oldP].category == newTs[newP].category &&
                        ts[oldP].description == newTs[newP].description
                )
    }

    override fun areContentsTheSame(oldP: Int, newP: Int): Boolean {
        return ts[oldP].id == newTs[newP].id
    }
}