package com.example.timebankingapp

import android.graphics.Color.rgb
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class Accepted_AssignedTimeslotList : Fragment() {

    private lateinit var tabAdapter: TabAdapter
    private lateinit var viewPager: ViewPager2

    private lateinit var tab0text: TextView
    private lateinit var tab1text: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tabAdapter = TabAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = tabAdapter
        val teal200 = rgb(128, 203, 196)
        val black = rgb(128,128,128)
        tab0text = view.findViewById(R.id.textTab0)
        tab1text = view.findViewById(R.id.textTab1)
        tab0text.text = getString(R.string.accepted)
        tab1text.text = getString(R.string.assigned)
        tab0text.setTextColor(teal200)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            if(pos == 0) {
                tab.text = "Accepted"
                if(tab.isSelected) tab0text.setTextColor(teal200)
                tab0text.setOnClickListener {
                    tabLayout.selectTab(tab)
                }
            }
            else  {
                tab.text = "Assigned"
                if(tab.isSelected) tab1text.setTextColor(teal200)
                tab1text.setOnClickListener {
                    tabLayout.selectTab(tab)
                }
            }
        }.attach()
        tabLayout!!.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                if (tab.position == 0) {
                    tab0text.setTextColor(teal200)
                    tab1text.setTextColor(black)
                } else {
                    tab0text.setTextColor(black)
                    tab1text.setTextColor(teal200)
                }
                val t = view.findViewById<ShapeableImageView>(R.id.tabIndicator)
                t.scaleX = -t.scaleX
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {

            }
            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })
    }
}

class TabAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = TabFragment()
        fragment.arguments = Bundle().apply {
            putInt("tab", position)
        }
        return fragment
    }

    class TabFragment : Fragment() {

        private val vm: MyViewModel by activityViewModels()

        private lateinit var recyclerView: RecyclerView
        private lateinit var myAdapter: TimeSlotAdapter

        private var mode: Boolean = false

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            return inflater.inflate(R.layout.fragment_accepted_assigned_timeslots, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            arguments?.getInt("tab")?.apply {

                //implementa l'adapter giusto in base alla tab
                registerForContextMenu(view)

                mode = this != 0

                view.findViewById<TextView>(R.id.headerBody).text =
                    if (this == 0) getString(R.string.accepted_header_body)
                    else getString(R.string.assigned_header_body)

                recyclerView = view.findViewById(R.id.recycler_view)
                recyclerView.layoutManager = LinearLayoutManager(context)

                myAdapter = TimeSlotAdapter(
                    mutableListOf(),
                    false,
                    { timeslot -> adapterOnClick(timeslot, this == 0) },
                    { timeslot -> adapterOnDeleteClick(timeslot) }
                )
                recyclerView.adapter = myAdapter

                (if (this == 0) vm.timeslots else vm.myTimeslots ).observe(viewLifecycleOwner) {
                    val timeslotList = mutableListOf<Timeslot>()
                    it.filter{ t-> t.accepted != "" }.forEach { t ->
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

                    myAdapter.setTimeslotList(timeslotList)

                }
            }
        }

        private fun adapterOnClick(timeSlot: Timeslot, boolean: Boolean) {
            val bundle = bundleOf("id" to timeSlot.id)
            findNavController().navigate(
                if (boolean) R.id.action_accepted_assigned_timeslotsList_to_timeslot
                else R.id.action_accepted_assigned_timeslotsList_to_myTimeslotDetails, bundle)
        }

        private fun adapterOnDeleteClick(timeSlot: Timeslot) {
            vm.remove(timeSlot.id)
        }
    }
}
