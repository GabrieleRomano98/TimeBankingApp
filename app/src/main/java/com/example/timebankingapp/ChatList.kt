package com.example.timebankingapp

import ChatAdapter
import ChatListItem
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ChatList : Fragment() {

    private lateinit var tab0text: TextView
    private lateinit var tab1text: TextView

    private lateinit var collectionAdapter: CollectionAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        collectionAdapter = CollectionAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        viewPager.adapter = collectionAdapter
        val teal200 = Color.rgb(128, 203, 196)
        val black = Color.rgb(128, 128, 128)
        tab0text = view.findViewById(R.id.textTab0)
        tab1text = view.findViewById(R.id.textTab1)
        tab0text.setTextColor(teal200)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            if(pos == 0) {
                tab.text = getString(R.string.other_s_timeslots)
                tab0text.text = getString(R.string.other_s_timeslots)
                if(tab.isSelected) tab0text.setTextColor(teal200)
                tab0text.setOnClickListener {
                    tabLayout.selectTab(tab)
                }
            }
            else  {
                tab.text = getString(R.string.your_timeslots)
                tab1text.text = getString(R.string.your_timeslots)
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

class CollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = ObjectFragment()
        fragment.arguments = Bundle().apply {
            putInt("tab", position)
        }
        return fragment
    }
}

class ObjectFragment : Fragment() {

    private val vm:MyViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: ChatAdapter

    private lateinit var chatToDelete: String
    private lateinit var deleteUpdate: () -> Unit
    private lateinit var dialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.chat_list_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initConfirmDeleteDialog()
        arguments?.getInt("tab")?.apply {
            recyclerView = view.findViewById(R.id.recycler_view)
            recyclerView.layoutManager = LinearLayoutManager(context)

            myAdapter = ChatAdapter(
                mutableListOf(),
                { chat -> adapterOnClick(chat) },
                { chat, update -> adapterOnDeleteClick(chat, update) },
                vm,
                viewLifecycleOwner
            )
            recyclerView.adapter = myAdapter

            vm.chats.observe(viewLifecycleOwner) { list ->
                val userId = Firebase.auth.currentUser!!.uid
                myAdapter.setChatList(list
                    .filter { userId == if(this == 0) it.interested else it.user }
                    .map{ ChatListItem(if(this == 0) it.user else it.interested, it.timeslot, it.id) }
                    .toMutableList()
                )
            }
        }
    }

    private fun adapterOnClick(id: String) {
        findNavController().navigate(R.id.action_chatList_to_chat2, bundleOf("chat" to id))
    }

    private fun adapterOnDeleteClick(id: String, update: () -> Unit) {
        chatToDelete = id
        deleteUpdate = update
        dialog.show()
    }

    private fun initConfirmDeleteDialog() {
        dialog = AlertDialog.Builder(context)
            .setPositiveButton("Confirm") { dialog, _ ->
                vm.deleteChat(chatToDelete)
                deleteUpdate()
                dialog.cancel()
            }
            .setNegativeButton("Cancel"){ dialog, _ ->
                dialog.cancel()
            }
            .setTitle("Wait!")
            .setMessage("Are you sure you want to delete this chat?")
            .create()
    }
}