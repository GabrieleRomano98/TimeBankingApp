package com.example.timebankingapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.slider.RangeSlider
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class TimeslotList : Fragment() {

    private val vm:MyViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: TimeSlotAdapter

    private var tsl = mutableListOf<Timeslot>()

    private var filterList = mutableListOf<(t: Timeslot) -> Boolean>()
    private var sortFun = {_: Timeslot, _: Timeslot -> 1}
    private var searchInput = ""

    private var filter: String = "all"

    private var startDateText: String = ""
    private var endDateText: String = ""
    private var startTimeText: String = ""
    private var endTimeText: String = ""
    private var minHText: Int = -1
    private var maxHText: Int = -1
    private var sortString: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timeslot_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Message when there are no timeslot items
        val sb = Snackbar.make(view, R.string.SnackBar_NoItems, LENGTH_SHORT)

        //Get the search view
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchView)

        if (arguments != null) {
            //Get filter flag to show FilteredTimeslot
            filter = requireArguments().getString("Category", "all")
            (activity as AppCompatActivity?)!!.supportActionBar!!.title = filter
        }

        registerForContextMenu(view)
        view.findViewById<ShapeableImageView>(R.id.sortButton).setOnClickListener {
            requireActivity().openContextMenu(it)
        }
        searchView.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                searchInput = newText
                setFilter()
                return true
            }
        })

        if(savedInstanceState != null)
            restoreData(savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        //headerMyAdapter = RecyclerViewHeaderAdapter(this.layoutInflater)
        myAdapter = TimeSlotAdapter(
            mutableListOf(),
            false,
            { timeslot -> adapterOnClick(timeslot) },
            { timeslot -> adapterOnDeleteClick(timeslot)}
        )
        //headerMyAdapter = RecyclerViewHeaderAdapter(this.layoutInflater)
        //recyclerView.adapter = ConcatAdapter(headerMyAdapter, myAdapter)
        recyclerView.adapter = myAdapter

        vm.timeslots.observe(viewLifecycleOwner) {
            val timeslotList = mutableListOf<Timeslot>()
            if (filter.lowercase(Locale.getDefault()) == "all") {
                it.filter{ t -> t.accepted == "" }.forEach { t ->
                    timeslotList.add(
                        Timeslot(
                            t.title,
                            t.description,
                            t.date,
                            t.place,
                            t.time,
                            t.hours.toInt(),
                            t.category,
                            t.id
                        )
                    )
                }
            } else {
                it.filter{ t ->
                    t.accepted == "" && t.category.lowercase(Locale.getDefault()) == filter.lowercase(Locale.getDefault())
                }.forEach { t ->
                    timeslotList.add(
                        Timeslot(
                            t.title,
                            t.description,
                            t.date,
                            t.place,
                            t.time,
                            t.hours.toInt(),
                            t.category,
                            t.id
                        )
                    )
                }
            }


            //Checking how many items there are in the list
            if (timeslotList.isEmpty()) {
                sb.show()
            } else {
                tsl = timeslotList
                setFilter()
                initFilterDialog(view.findViewById<ShapeableImageView>(R.id.filterButton))
            }

        }

    }

    private fun adapterOnClick(timeSlot: Timeslot) {
        val bundle = bundleOf("id" to timeSlot.id)
        findNavController().navigate(R.id.action_timeslotList_to_timeslot, bundle)
    }

    private fun adapterOnDeleteClick(timeSlot: Timeslot) {
        vm.remove(timeSlot.id)
    }

    private fun initFilterDialog(btn: View) {
        //Create the view for the dialog
        val v = layoutInflater.inflate(R.layout.dialog_filter, null)
        //Initialize the values to the last set
        val startDate = v.findViewById<TextView>(R.id.start_date)
        startDate.text = startDateText
        val endDate = v.findViewById<TextView>(R.id.end_date)
        endDate.text = endDateText
        val startTime = v.findViewById<TextView>(R.id.start_time)
        startTime.text = startDateText
        val endTime = v.findViewById<TextView>(R.id.end_time)
        endTime.text = endTimeText

        val minH = v.findViewById<TextView>(R.id.min_val)
        val min = if(minHText == -1) 0 else minHText
        minHText = min
        val maxH = v.findViewById<TextView>(R.id.max_val)
        val max = if(maxHText == -1) tsl.fold(0) { acc, ts -> max(acc, ts.hours) } else maxHText
        maxHText = max
        maxH.text = max.toString()
        val range = v.findViewById<RangeSlider>(R.id.hour_values)
        range.valueTo = tsl.fold(0) { acc, ts -> max(acc, ts.hours) }.toFloat()
        range.values = listOf(min.toFloat(), max.toFloat())
        range.addOnChangeListener { _, _, _ ->
            minH.text = range.values[0].toInt().toString()
            maxH.text = range.values[1].toInt().toString()
        }
        //Init date and time pickers
        initTimePicker(startTime)
        initTimePicker(endTime)
        initDatePicker(startDate)
        initDatePicker(endDate)
        // Build the dialog
        val d = AlertDialog.Builder(context)
            .setPositiveButton("Confirm"){ dialog, _ ->
                startDateText = startDate.text.toString()
                endDateText = endDate.text.toString()
                startTimeText = startTime.text.toString()
                endTimeText = endTime.text.toString()
                minHText = minH.text.toString().toInt()
                maxHText = maxH.text.toString().toInt()
                filterList = mutableListOf(
                    {if(startDate.text != "") dateFromString(it.date).after(dateFromString(startDate.text.toString())) else true},
                    {if(endDate.text != "") dateFromString(it.date).before(dateFromString(endDate.text.toString())) else true},
                    {if(startTime.text != "") timeFromString(it.time).before(timeFromString(startTime.text.toString())) else true},
                    {if(endTime.text != "") timeFromString(it.time).before(timeFromString(endTime.text.toString())) else true},
                    {it.hours >= minH.text.toString().toInt()},
                    {it.hours <= maxH.text.toString().toInt()}
                )
                setFilter()
                dialog.cancel()
            }
            .setNeutralButton("Clear"){ dialog, _ ->
                startDateText = ""
                startDate.text = ""
                endDateText = ""
                endDate.text = ""
                startTimeText = ""
                startTime.text = ""
                endTimeText = ""
                endTime.text = ""
                minH.text = "0"
                minHText = -1
                val m =  tsl.fold(0) { acc, ts -> max(acc, ts.hours) }
                maxH.text = m.toString()
                maxHText = -1
                range.valueTo = m.toFloat()
                range.values = listOf(0f, m.toFloat())
                filterList = mutableListOf()
                setFilter()
                dialog.cancel()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                startDate.text =startDateText
                endDate.text = endDateText
                startTime.text = startTimeText
                endTime.text = endTimeText
                minH.text = minHText.toString()
                maxH.text = maxHText.toString()
                range.values = listOf(minHText.toFloat(), maxHText.toFloat())
                dialog.cancel()
            }
            .create()
        if(v.parent != null)
            (v.parent as ViewGroup).removeView(v)
        d.setView(v)
        btn.setOnClickListener{
            d.show()
        }
    }

    private fun initTimePicker(time: TextView) {
        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, h, m ->
            val t = "${String.format("%02d", h)}:${String.format("%02d", m)}"
            time.text = t
        }
        val cal = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            requireContext(), AlertDialog.THEME_HOLO_LIGHT, timeSetListener,
            cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE], true
        )
        //clear button
        timePickerDialog.setOnCancelListener { time.text = "" }
        val clearListener = DialogInterface.OnClickListener {_,_ -> time.text = "" }
        val cancelListener = DialogInterface.OnClickListener {_,_ -> ; }
        timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", cancelListener)
        timePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "CLEAR", clearListener)
        time.setOnClickListener{ timePickerDialog.show() }
    }

    private fun initDatePicker(date: TextView) {
        val dateSetListener = DatePickerDialog.OnDateSetListener{_, year, month, day ->
            val d = "$day ${getMonthFormat(month)} $year"
            date.text = d
        }
        val cal = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(), AlertDialog.THEME_HOLO_LIGHT, dateSetListener,
            cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]
        )
        //clear button
        val clearListener = DialogInterface.OnClickListener {_,_ ->
            date.text = ""
        }
        val cancelListener = DialogInterface.OnClickListener {_,_ -> ; }
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", cancelListener)
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "CLEAR", clearListener)
        //show picker
        date.setOnClickListener{ datePickerDialog.show() }
    }

    private fun setFilter() {
        myAdapter.setTimeslotList(tsl
            .sortedWith(sortFun)
            .filter{it.title.lowercase(Locale.getDefault()).startsWith(searchInput.lowercase(Locale.getDefault()))}
            .filter{ filterList.fold(true){ acc, func -> acc && func(it) }
            } as MutableList<Timeslot>)
    }

    private fun dateFromString(d: String): Date {
        val formatter = SimpleDateFormat("dd MMM yyyy")
        return formatter.parse(d)!!
    }

    private fun timeFromString(d: String): Date {
        val formatter = SimpleDateFormat("HH:mm")
        return formatter.parse(d)!!
    }

    private fun getMonthFormat(month: Int): String {
        if (month == 0) return "JAN"
        if (month == 1) return "FEB"
        if (month == 2) return "MAR"
        if (month == 3) return "APR"
        if (month == 4) return "MAY"
        if (month == 5) return "JUN"
        if (month == 6) return "JUL"
        if (month == 7) return "AUG"
        if (month == 8) return "SEP"
        if (month == 9) return "OCT"
        if (month == 10) return "NOV"
        return "DEC"
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if(activity != null)
            requireActivity().menuInflater.inflate(R.menu.sort_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        updateSort(item.title.toString())
        setFilter()
        return true
    }

    private fun updateSort(s: String) {
        sortString = s
        when(s) {
            "Hours" -> sortFun = { t1, t2 -> when(true) {
                (t1.hours > t2.hours) ->  1
                (t1.hours < t2.hours) -> -1
                else -> 0
            }}
            "Date" -> sortFun = { t1, t2 -> when(true) {
                dateFromString(t1.date).after(dateFromString(t2.date)) -> 1
                dateFromString(t1.date).before(dateFromString(t2.date)) -> -1
                else -> 0
            }}
            "Time" -> sortFun = { t1, t2 -> when(true) {
                timeFromString(t1.time).after(timeFromString(t2.time)) -> 1
                timeFromString(t1.time).before(timeFromString(t2.time)) -> -1
                else -> 0
            }}
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("startDateText", startDateText)
        outState.putString("endDateText", endDateText)
        outState.putString("startTimeText", startTimeText)
        outState.putString("endTimeText", endTimeText)
        outState.putInt("minHText", minHText)
        outState.putInt("maxHText", maxHText)
        outState.putString("searchInput", searchInput)
        outState.putString("sortString", sortString)
    }

    private fun restoreData(b: Bundle) {
        startDateText = b.getString("startDateText", "")
        endDateText = b.getString("endDateText", "")
        startTimeText = b.getString("startTimeText", "")
        endTimeText = b.getString("endTimeText", "")
        minHText = b.getInt("minHText", -1)
        maxHText = b.getInt("maxHText", -1)
        filterList = mutableListOf(
            {if(startDateText != "") dateFromString(it.date).after(dateFromString(startDateText)) else true},
            {if(endDateText != "") dateFromString(it.date).before(dateFromString(endDateText)) else true},
            {if(startTimeText != "") timeFromString(it.time).before(timeFromString(startTimeText)) else true},
            {if(endTimeText != "") timeFromString(it.time).before(timeFromString(endTimeText)) else true},
            {it.hours >= minHText},
            {it.hours <= maxHText}
        )
        searchInput = b.getString("searchInput", "")
        updateSort(b.getString("sortString", ""))
    }

}