import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.timebankingapp.*
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.math.max

data class ChatListItem(
    val user: String,
    val timeslot: String,
    val id: String
)

class ChatAdapter(
    private var chatList: MutableList<ChatListItem>,
    private val onClick: (String) -> Unit,
    private val onDelete: (String, () -> Unit) -> Unit,
    private val vm: MyViewModel,
    private val owner: LifecycleOwner
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    fun setChatList(newChats: MutableList<ChatListItem>) {
        val diffs = DiffUtil.calculateDiff(
            ChatDiffCallback(chatList, newChats)
        )
        chatList = newChats
        diffs.dispatchUpdatesTo(this)
    }

    class ChatViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        fun bind(
            chat: ChatListItem,
            onClick: (String) -> Unit,
            onDelete: (String) -> Unit,
            vm: MyViewModel,
            owner: LifecycleOwner
        ) {
            (if(chat.id.startsWith("[${Firebase.auth.currentUser!!.uid}]"))
                vm.timeslots
            else vm.myTimeslots)
            .observe(owner) {
                view.findViewById<TextView>(R.id.timeslotTitle).text =
                    it.find { t -> t.id == chat.timeslot }?.title
            }
            vm.getProfileById(chat.user).observe(owner) {
                view.findViewById<TextView>(R.id.usernameChat).text = it.username
                vm.getProfileImage(chat.user).observe(owner) { img ->
                    view.findViewById<ShapeableImageView>(R.id.profileImage).setImageBitmap(img)
                }
            }
            view.findViewById<ShapeableImageView>(R.id.deleteIcon).setOnClickListener {
                onDelete(chat.id)
            }
            view.setOnClickListener {
                onClick(chat.id)
            }
        }

        fun unbind(view: View) {
            view.setOnClickListener(null)
            view.findViewById<ShapeableImageView>(R.id.deleteIcon).setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_list_item, parent, false)
        return ChatViewHolder(vg)
    }

    override fun onViewRecycled(holder: ChatViewHolder) {
        holder.unbind(holder.itemView)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.bind(
            chat,
            onClick,
            {
                onDelete(chat.id) {
                    val pos = chatList.indexOf(chat)
                    if (pos != -1) {
                        chatList.removeAt(pos)
                        notifyItemRemoved(pos)
                    }
                }},
            vm, owner)
    }
}

class ChatDiffCallback(private val chat: List<ChatListItem>, private val newChat: List<ChatListItem>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = chat.size
    override fun getNewListSize(): Int = newChat.size
    override fun areItemsTheSame(oldP: Int, newP: Int): Boolean {
        return (
            chat[oldP].user == newChat[newP].user &&
            chat[oldP].timeslot == newChat[newP].timeslot
        )
    }
    override fun areContentsTheSame(oldP: Int, newP: Int): Boolean {
        if(max(oldP, newP) >= chat.size) return false
        return chat[oldP].id == newChat[newP].id
    }
}