package com.example.timebankingapp

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

class MessageAdapter(
    private var messageList: MutableList<Message>,
    private val acceptOffer: (value: Int) -> Unit,
    private val retireOffer: (date: Long) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var accepted: Boolean = false

    fun setChatList(newMessages: MutableList<Message>) {
        val diffs = DiffUtil.calculateDiff(
            MessageDiffCallback(messageList, newMessages)
        )
        messageList = newMessages
        diffs.dispatchUpdatesTo(this)
    }

    fun setAccepted() {
        accepted = true
        messageList.filter{ it.offer && it.user }.forEach{
            notifyItemChanged(messageList.indexOf(it))
        }
    }

    class MessageViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        private val acceptButton = view.findViewById<Button>(R.id.acceptButton)
        fun bind(
            message: Message,
            acceptOffer: (value: Int) -> Unit,
            retireMessage: (date: Long) -> Unit,
            accepted: Boolean
        ) {
            if(!message.user) {
                view.findViewById<LinearLayout>(R.id.row).gravity = Gravity.START
                val white = Color.argb(255, 255, 255, 255)
                view.findViewById<CardView>(R.id.messageCard).background.setTint(white)
                view.findViewById<TextView>(R.id.content)
                    .setTextColor(Color.argb(255, 0, 0, 0))
                view.findViewById<TextView>(R.id.time)
                    .setTextColor(Color.argb(255, 0, 0, 0))
            }
            else {
                view.findViewById<LinearLayout>(R.id.row).gravity = Gravity.END
                val teal700 = Color.argb(255, 0, 121, 107)
                view.findViewById<CardView>(R.id.messageCard).background.setTint(teal700)
                view.findViewById<TextView>(R.id.content)
                    .setTextColor(Color.argb(255, 255, 255, 255))
                view.findViewById<TextView>(R.id.time)
                    .setTextColor(Color.argb(255, 255, 255, 255))
            }
            if(message.offer) {
                acceptButton.visibility = View.VISIBLE
                acceptButton.setOnClickListener {
                    if(!message.user) acceptOffer(message.content.toInt())
                    else retireMessage(message.date)
                }
                if(message.user) {
                    acceptButton.setTextColor(Color.argb(255, 139, 0, 0))
                    acceptButton.text = view.context.getString(R.string.retire)
                }
                if(accepted)
                    acceptButton.isEnabled = false
            }
            else acceptButton.visibility = View.GONE
            (message.content + if(message.offer) " Hours" else "").also {
                view.findViewById<TextView>(R.id.content).text = it }
            val time = SimpleDateFormat("hh:mm").format(Date(message.date))
            view.findViewById<TextView>(R.id.time).text = time
        }

        fun unbind(view: View) {
            view.setOnClickListener(null)
            acceptButton.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return MessageViewHolder(vg)
    }

    override fun onViewRecycled(holder: MessageViewHolder) {
        holder.unbind(holder.itemView)
    }

    override fun getItemCount(): Int = messageList.size

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.bind(message, acceptOffer, retireOffer, accepted)
    }
}

class MessageDiffCallback(private val msg: List<Message>, private val newMsg: List<Message>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = msg.size
    override fun getNewListSize(): Int = newMsg.size
    override fun areItemsTheSame(oldP: Int, newP: Int): Boolean {
        return (
            msg[oldP].user == newMsg[newP].user &&
            msg[oldP].content == newMsg[newP].content &&
            msg[oldP].date == newMsg[newP].date
        )
    }
    override fun areContentsTheSame(oldP: Int, newP: Int): Boolean {
        if(max(oldP, newP) >= msg.size) return false
        return "${msg[oldP].user}${msg[oldP].date}" == "${msg[newP].user}${msg[newP].date}"
    }
}