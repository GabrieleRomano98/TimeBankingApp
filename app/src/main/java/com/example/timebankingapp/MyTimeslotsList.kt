package com.example.timebankingapp

import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar


class MyTimeslotsList : Fragment() {

    private val vm:MyViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: TimeSlotAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_timeslots_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Message when there are no timeslot items
        val sb = Snackbar.make(view, R.string.SnackBar_NoMyItems, LENGTH_SHORT)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        myAdapter = TimeSlotAdapter(
            mutableListOf(),
            true,
            { timeslot -> adapterOnClick(timeslot) },
            { timeslot -> adapterOnDeleteClick(timeslot) }
        )
        recyclerView.adapter = myAdapter

        //Floating Button to create new Timeslot
        val orientation = this.resources.configuration.orientation
        val fab = if (orientation == Configuration.ORIENTATION_PORTRAIT)
            view.findViewById<FloatingActionButton>(R.id.fab)
        else view.findViewById<Button>(R.id.button)
        fab.setOnClickListener {
            findNavController().navigate(R.id.action_timeslotList_to_createTimeslot)
        }

        vm.myTimeslots.observe(viewLifecycleOwner) {
            val timeslotList = mutableListOf<Timeslot>()

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

            //Checking how many items there are in the list
            if (timeslotList.isEmpty()) {
                sb.show()
            } else {
                myAdapter.setTimeslotList(timeslotList)
            }

        }

    }

    private fun adapterOnClick(timeSlot: Timeslot) {
        val bundle = bundleOf("id" to timeSlot.id)
        findNavController().navigate(R.id.action_timeslotList_to_myTimeslotDetails, bundle)
    }

    private fun adapterOnDeleteClick(timeSlot: Timeslot) {
        vm.remove(timeSlot.id)
    }

}