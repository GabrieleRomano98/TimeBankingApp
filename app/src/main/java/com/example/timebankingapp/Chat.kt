package com.example.timebankingapp

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Chat : Fragment() {

    private val vm: MyViewModel by activityViewModels()

    private lateinit var timeslot: TimeslotModel

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: MessageAdapter

    private lateinit var timeslotId: String
    private lateinit var owner: String
    private lateinit var user: String
    private lateinit var chatId: String

    private lateinit var text: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val d1 = AlertDialog.Builder(context).create()
        timeslotId = requireArguments().getString("timeslot", "")
        owner = requireArguments().getString("owner", "")
        user = requireArguments().getString("user", "")
        chatId = requireArguments().getString("chat")?:
            "[${Firebase.auth.currentUser?.uid}][${timeslotId}]"
        recyclerView = view.findViewById(R.id.chat_list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        myAdapter = MessageAdapter(
            mutableListOf(),
            { vm.acceptOffer(it.toLong(), timeslotId, user, owner) { fail ->
                d1.setButton(DialogInterface.BUTTON_POSITIVE, "Ok") { _, _ ->
                    if(!fail) findNavController().popBackStack()
                }
                if(fail) {
                    val imOwner = Firebase.auth.currentUser!!.uid == owner
                    val start = if(imOwner) "The other user does" else "You do"
                    d1.setTitle("$start not have enough hours!")
                    d1.setMessage(
                        if(imOwner) "Wait for him to gain some hours or try a lower offer"
                        else "Offer a new timeslot to gain hours or try a lower offer"
                    )
                }
                else {
                    d1.setTitle("Offer accepted")
                    d1.setMessage("Do not forget to review the other user when the timeslot is completed!")
                }
                d1.show()
            } },
            { vm.retireOffer(it, timeslotId, chatId)}
        )
        recyclerView.adapter = myAdapter
        vm.chats.observe(viewLifecycleOwner) { list ->
            if(timeslotId == "") {
                list.find { it.id == chatId }.also {
                    timeslotId = it!!.timeslot
                    owner = it.user
                    user = it.interested
                }
            }
            (if(owner == Firebase.auth.currentUser!!.uid) vm.myTimeslots else vm.timeslots)
                .observe(viewLifecycleOwner){ l ->
                    if(l.find{ it.id == timeslotId }?.accepted != "") {
                        myAdapter.setAccepted()
                        view.findViewById<Button>(R.id.offer_button).visibility = View.GONE
                    }
                }
            myAdapter.setChatList(vm.getMessageFromChat(timeslotId, owner, chatId)
                .sortedWith { m1, m2 ->
                    when (true) {
                        m1.date > m2.date -> 1
                        m1.date < m2.date -> -1
                        else -> 0
                    }
                }.toMutableList())
            if(myAdapter.itemCount != 0)
                recyclerView.scrollToPosition(myAdapter.itemCount - 1)
        }
        recyclerView.addOnLayoutChangeListener{ _, _, _, _, _, _, _, _, _ ->
            if(myAdapter.itemCount != 0)
                recyclerView.smoothScrollToPosition(myAdapter.itemCount - 1)
        }

        vm.timeslots.observe(viewLifecycleOwner) { list ->
            timeslot = list.find { it.id == timeslotId }?: TimeslotModel()
        }

        text = view.findViewById(R.id.message_text)
        view.findViewById<ShapeableImageView>(R.id.send_button).setOnClickListener {
            vm.addMessage(text.text.toString(), timeslotId, false, chatId)
            text.text = ""
        }
        val d = initOfferDialog()
        view.findViewById<Button>(R.id.offer_button).setOnClickListener {
            d.show()
        }
        if (savedInstanceState != null)
            restoreData(savedInstanceState)
    }

    private fun initOfferDialog(): AlertDialog {
        val v = layoutInflater.inflate(R.layout.offer_dialog, null)
        val d = AlertDialog.Builder(context)
            .setPositiveButton("Send") { dialog, _ ->
                vm.addMessage(
                    v.findViewById<EditText>(R.id.hoursOffered).text.toString(),
                    timeslotId,true, chatId)
                dialog.cancel()
            }
            .setNegativeButton("Cancel"){ dialog, _ ->
                dialog.cancel()
            }
            .create()
        if(v.parent != null)
            (v.parent as ViewGroup).removeView(v)
        d.setView(v)
        return d
    }

    private fun restoreData(savedInstanceState: Bundle) {
        text.text = savedInstanceState.getString("text")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("text", text.text.toString())
    }
}