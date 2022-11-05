package com.example.timebankingapp

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar


class InterestedTimeslotsList : Fragment() {

    private val vm:MyViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: TimeSlotAdapter



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_interested_timeslot_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Message when there are no timeslot items
        val sb = Snackbar.make(view, R.string.SnackBar_NoItems_interested, LENGTH_SHORT)

        registerForContextMenu(view)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        myAdapter = TimeSlotAdapter(
            mutableListOf(),
            false,
            { timeslot -> adapterOnClick(timeslot) },
            { timeslot -> adapterOnDeleteClick(timeslot) }
        )

        recyclerView.adapter = myAdapter

        vm.getInterested().observe(viewLifecycleOwner) {
            val timeslotList = mutableListOf<Timeslot>()
                it.forEach { t ->
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
            }

            myAdapter.setTimeslotList(timeslotList)

        }

    }

    private fun adapterOnClick(timeSlot: Timeslot) {
        val bundle = bundleOf("id" to timeSlot.id)
        bundle.putBoolean("modeNO", true)
        findNavController().navigate(R.id.action_interested_timeslotsList_to_timeslot, bundle)
    }

    private fun adapterOnDeleteClick(timeSlot: Timeslot) {
        vm.remove(timeSlot.id)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        if(activity != null)
            requireActivity().menuInflater.inflate(R.menu.sort_menu, menu)
    }

}