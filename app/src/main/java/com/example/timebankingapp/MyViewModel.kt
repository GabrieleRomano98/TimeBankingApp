package com.example.timebankingapp

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class MyViewModel(application: Application) : AndroidViewModel(application) {

    val timeslots = MutableLiveData<List<TimeslotModel>>()
    val myTimeslots = MutableLiveData<List<TimeslotModel>>()
    val chats = MutableLiveData<MutableList<ChatM>>()
    val userAuth = MutableLiveData<Profile>()
    val imageAuth = MutableLiveData<Bitmap>()

    private lateinit var l: ListenerRegistration
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val imagesRef = Firebase.storage.reference.child("profile_images")
    private val timeslotImagesRef = Firebase.storage.reference.child("timeslot_images")

    init {
        initMyViewModel()
    }

    fun getInterested(): MutableLiveData<List<TimeslotModel>> =
        MutableLiveData<List<TimeslotModel>>(timeslots.value!!.filter {
            userAuth.value!!.interested.contains(
                it.id
            )
        })

    fun initMyViewModel() {
        val firebaseUser = Firebase.auth.currentUser
        if (firebaseUser != null) {
            val currentUser = firebaseUser.uid

            l = db.collection("timeslots")
                .whereEqualTo("user", currentUser)
                .addSnapshotListener { v, e ->
                    if (e == null) {
                        myTimeslots.value = v?.mapNotNull {
                            it.toTimeslot()
                        }
                    } else myTimeslots.value = emptyList()
                }

            l = db.collection("timeslots")
                .whereNotEqualTo("user", currentUser)
                .whereIn("accepted", listOf("", currentUser))
                .addSnapshotListener { v, e ->
                    if (e == null) {
                        timeslots.value = v?.mapNotNull { it.toTimeslot() }
                    } else timeslots.value = emptyList()
                }

            l = db.collection("chat")
                .whereEqualTo("user", currentUser)
                .addSnapshotListener { v, e ->
                    if (e == null && v != null) {
                        val x = chats.value?.toMutableList()?: mutableListOf()
                        val new = v.mapNotNull { it.toChatM() }
                        x.removeAll {  it.user == currentUser }
                        new.forEach { c ->
                            x.add(c)
                        }
                        chats.value = x
                    }
                }
            l = db.collection("chat")
                .whereEqualTo("interested", currentUser)
                .addSnapshotListener { v, e ->
                    if (e == null && v != null) {
                        val x = chats.value?.toMutableList()?: mutableListOf()
                        val new = v.mapNotNull { it.toChatM() }
                        x.removeAll {  it.interested == currentUser }
                        new.forEach { c ->
                            x.add(c)
                        }
                        chats.value = x
                    }
                }

            // Load current user's details
            l = db.collection("users")
                .document(currentUser)
                .addSnapshotListener { v, e ->
                    userAuth.value = if (e == null && Firebase.auth.currentUser != null) {
                        val x = v?.toProfile()
                        val f = File.createTempFile("profile", "jpg")
                        imagesRef.child("${x?.id}.jpg").getFile(f).addOnSuccessListener {
                            imageAuth.value = BitmapFactory.decodeFile(f.absolutePath)
                        }
                        x
                    } else {
                        Profile()
                    }
                }
        }
        else userAuth.value = Profile()
    }

    fun add(
        title: String, description: String, date: String,
        time: String, hours: Number, place: String,
        category: String, image: Bitmap
    ) {
        db
            .collection("timeslots")
            .add(
                mapOf(
                    "title" to title,
                    "description" to description,
                    "date" to date,
                    "time" to time,
                    "hours" to hours,
                    "place" to place,
                    "image" to 0,
                    "user" to Firebase.auth.currentUser?.uid,
                    "username" to userAuth.value?.username,
                    "category" to category,
                    "accepted" to ""
                )
            )
            .addOnSuccessListener { addTimeslotImage(image, it.id) }
            .addOnFailureListener { println("error") }

    }

    fun remove(id: String) = db.collection("timeslots").document(id).delete()

    fun update(
        id: String,
        title: String,
        hours: Int,
        place: String,
        time: String,
        date: String,
        description: String,
        category: String,
        image: Bitmap
    ) {
        db
            .collection("timeslots")
            .document(id)
            .update(
                mapOf(
                    "title" to title,
                    "description" to description,
                    "date" to date,
                    "time" to time,
                    "hours" to hours,
                    "place" to place,
                    "category" to category
                )
            )
            .addOnSuccessListener { addTimeslotImage(image, id) }
            .addOnFailureListener { Log.d("Firebase", it.message ?: "Error") }
    }

    fun getProfileById(id: String): LiveData<Profile> {
        val x = MutableLiveData<Profile>()
        l = db.collection("users")
            .document(id)
            .addSnapshotListener { v, e ->
                x.value = if (e == null) {
                    v?.toProfile()
                } else Profile()
            }
        return x
    }

    fun getProfileImage(id: String = Firebase.auth.currentUser!!.uid): LiveData<Bitmap> {
        val x = MutableLiveData<Bitmap>()
        val f = File.createTempFile("profile", "jpg")
        imagesRef.child("$id.jpg").getFile(f).addOnSuccessListener {
            x.value = BitmapFactory.decodeFile(f.absolutePath)
        }
        return x
    }

    fun getTimeslotImage(id: String): LiveData<Bitmap> {
        val x = MutableLiveData<Bitmap>()
        val f = File.createTempFile("timeslot", "jpg")
        timeslotImagesRef.child("$id.jpg").getFile(f).addOnSuccessListener {
            x.value = BitmapFactory.decodeFile(f.absolutePath)
        }
        return x
    }

    fun addProfile(
        surname: String,
        name: String,
        username: String,
        location: String,
        description: String,
        skills: List<String>,
        hours: Number? = userAuth.value?.hours,
        image: Number? = userAuth.value?.image
    ) {
        val id = Firebase.auth.currentUser!!.uid
        db
            .collection("users")
            .document(id)
            .set(
                mapOf(
                    "name" to name,
                    "surname" to surname,
                    "username" to username,
                    "email" to Firebase.auth.currentUser!!.email,
                    "location" to location,
                    "description" to description,
                    "skills" to skills,
                    "image" to (image?.toInt()?.plus(1))?.rem(3),
                    "hours" to hours,
                    "interested" to emptyList<String>()
                )
            )
            .addOnSuccessListener { Log.d("Firebase,", "success ") }
            .addOnFailureListener { Log.d("Firebase", it.message ?: "Error") }

    }

    fun updateProfile(
        surname: String,
        name: String,
        username: String,
        location: String,
        description: String,
        skills: List<String>,
        hours: Number? = userAuth.value?.hours,
        image: Number? = userAuth.value?.image
    ) {
        val id = Firebase.auth.currentUser!!.uid
        db
            .collection("users")
            .document(id)
            .update(
                mapOf(
                    "name" to name,
                    "surname" to surname,
                    "username" to username,
                    "email" to Firebase.auth.currentUser!!.email,
                    "location" to location,
                    "description" to description,
                    "skills" to skills,
                    "image" to (image?.toInt()?.plus(1))?.rem(3),
                    "hours" to hours,
                    "interested" to listOf<TimeslotModel>()
                )
            )
            .addOnSuccessListener { Log.d("Firebase,", "success ") }
            .addOnFailureListener { Log.d("Firebase", it.message ?: "Error") }

    }

    fun updateProfileImage(image: Bitmap) {
        val id = Firebase.auth.currentUser!!.uid
        val x = MutableLiveData<Bitmap>()
        val f = File.createTempFile(id, "jpg")
        x.value = BitmapFactory.decodeFile(f.absolutePath)
        val bos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val imgBA = bos.toByteArray()
        val fos = FileOutputStream(f)
        fos.write(imgBA)
        imagesRef.child("$id.jpg").putFile(Uri.fromFile(f))
        imageAuth.value = image
    }

    private fun addTimeslotImage(image: Bitmap, id: String) {
        val x = MutableLiveData<Bitmap>()
        val f = File.createTempFile(id, "jpg")
        x.value = BitmapFactory.decodeFile(f.absolutePath)
        val bos = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, bos)
        val imgBA = bos.toByteArray()
        val fos = FileOutputStream(f)
        fos.write(imgBA)
        timeslotImagesRef.child("$id.jpg").putFile(Uri.fromFile(f))
    }

    fun isRegistered(email: String, action: (b: Boolean) -> Unit) {
        var x = ListenerRegistration {}
        x = db.collection("users")
            .whereEqualTo("email", email)
            .addSnapshotListener { v, e ->
                if (e == null) {
                    action(!v!!.isEmpty)
                }
                x.remove()
            }
    }

    fun getMessageFromChat(timeslot: String, owner: String, id: String): MutableList<Message> {
        val currentUser = Firebase.auth.currentUser ?: return mutableListOf()
        val chat = chats.value!!.find{ it.id ==  id}
        return if(chat == null){
            val emptyMapArray = emptyList<Map<String, *>>()
            db.collection("chat")
                .document(id)
                .set(mapOf(
                    "timeslot" to timeslot,
                    "user" to owner,
                    "interested" to currentUser.uid,
                    "messages" to emptyMapArray,
                    "replies" to emptyMapArray))
            mutableListOf()
        }
        else {
            val res = mutableListOf<Message>()
            val my = currentUser.uid == chat.user
            chat.messages.forEach {
                res.add(Message(
                    !my,
                    it["content"] as String,
                    it["date"] as Long,
                    it["offer"] as Boolean
                ))
            }
            chat.replies.forEach {
                res.add(
                    Message(
                        my,
                        it["content"] as String,
                        it["date"] as Long,
                        it["offer"] as Boolean
                    )
                )
            }
            res
        }
    }

    fun addMessage(content: String, timeslot: String, offer: Boolean, chatId: String) {
        val new = FieldValue.arrayUnion(mapOf(
            "date" to Calendar.getInstance().timeInMillis,
            "content" to content,
            "offer" to offer
        ))
        val field = if(chatId.startsWith("[${userAuth.value!!.id}]")) "messages" else "replies"
        db.collection("chat")
            .document(chatId)
            .update(field, new)
    }

    fun acceptOffer(value: Long, timeslot: String, user:String, owner: String, result: (res: Boolean) -> Unit) {
        if(userAuth.value!!.hours.toInt() < value) {
            result(true)
            return
        }
        db.runBatch {
            it.update(
                db.collection("timeslots").document(timeslot),
                "accepted", user
            )
            val users = db.collection("users")
            it.update(users.document(user), "hours", FieldValue.increment(-value))
            it.update(users.document(owner), "hours", FieldValue.increment(value))
        }.addOnSuccessListener { result(false) }
    }

    fun retireOffer(date: Long, timeslot: String?, chatId: String = "[${userAuth.value!!.id}][${timeslot}]") {
        val c = chats.value!!.find{ it.id == chatId }
        val x = (if(chatId.startsWith("[${userAuth.value!!.id}]")) c?.messages else c?.replies)
            ?: return
        val elToRem = FieldValue.arrayRemove(
            x.find { it["date"] == date })
        db.collection("chat")
            .document(chatId)
            .update(
                if(chatId.startsWith("[${userAuth.value!!.id}]")) "messages" else "replies",
                elToRem)
    }

    fun deleteChat(chatId: String) = db.collection("chat").document(chatId).delete()

    fun addFavorite(timeslot: String) {
        val id = Firebase.auth.currentUser!!.uid
        val new = FieldValue.arrayUnion(
            timeslot
        )
        db
            .collection("users")
            .document(id)
            .update(
                    "interested", new
            )
            .addOnSuccessListener { Log.d("Firebase,", "success ") }
            .addOnFailureListener { Log.d("Firebase", it.message ?: "Error") }
    }

    fun removeFavorite(timeslot: String) {
        val id = Firebase.auth.currentUser!!.uid
        val new = FieldValue.arrayRemove(
            timeslot
        )
        db
            .collection("users")
            .document(id)
            .update(
                "interested", new
            )
            .addOnSuccessListener { Log.d("Firebase,", "success ") }
            .addOnFailureListener { Log.d("Firebase", it.message ?: "Error") }
    }

    //REVIEWS SECTION

    fun getAllReviews(): MutableLiveData<MutableList<Review>?> {
        val r = MutableLiveData<MutableList<Review>?>()
        //GET REVIEWS COLLECTION
        l = db.collection("reviews")
            .addSnapshotListener { value, error ->
                if(error == null) {
                    val reviews = mutableListOf<Review>()
                    value?.mapNotNull {
                        it.toReview()?.let { review ->
                            reviews.add(review)
                        }
                    }
                    Log.d("getAllReviews", reviews.toString())
                    r.value = reviews
                }else {
                    Log.d("getAllReviewsError", error.toString())
                    r.value = mutableListOf()
                }
            }
        return r
    }

    fun getReviews(receiverId: String, asWorker: Boolean): LiveData<MutableList<Review>?> {
        val r = MutableLiveData<MutableList<Review>?>()
        //GET REVIEWS COLLECTION
        l = db.collection("reviews")
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("asWorker", asWorker)
            .addSnapshotListener { v, e ->
                if (e == null) {
                    val reviews = mutableListOf<Review>()
                    v?.mapNotNull {
                        it.toReview()?.let { it1 ->

                            //SIMULATE A JOIN TO GET PROFILE INFO
                            db.collection("users")
                                .document(it1.senderId)
                                .get()
                                .addOnSuccessListener { document ->
                                    if (document != null) {

                                        //GET USERNAME
                                        val profile = document.toProfile()
                                        Log.d("Profile", profile.toString())
                                        it1.senderUsername = profile?.username.toString()

                                        //GET BITMAP IMAGE
                                        val f = File.createTempFile("profile", "jpg")
                                        imagesRef.child("${it1.senderId}.jpg").getFile(f)
                                            .addOnSuccessListener {
                                                it1.senderProfileImage =
                                                    BitmapFactory.decodeFile(f.absolutePath)

                                                //ALL RIGHT
                                                Log.d("getReviews", it1.toString())
                                                reviews.add(it1)

                                                r.value = reviews
                                            }
                                    } else {
                                        Log.d("Error", "No such document")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.d("get failed with ", exception.toString())
                                }
                        }
                    }
                } else r.value = null
            }
        return r
    }


    fun addReview(
        rating: Number,
        text: String,
        receiverId: String,
        timeslotId: String,
        senderId: String,
        date: String,
        asWorker: Boolean,
    ) {
        db
            .collection("reviews")
            .add(
                mapOf(

                    "senderId" to senderId,
                    "text" to text,
                    "date" to date,
                    "receiverId" to receiverId,
                    "timeslotId" to timeslotId,
                    "rating" to rating,
                    "asWorker" to asWorker,
                )
            )
            .addOnSuccessListener { }
            .addOnFailureListener { println("error") }
    }
}

data class TimeslotModel(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val hours: Number = 0,
    val place: String = "",
    val category: String = "",
    val user: String = "",
    val accepted: String = ""
)

fun DocumentSnapshot.toTimeslot(): TimeslotModel? {
    return try {
        val title = get("title") as String
        val description = get("description") as String
        val date = get("date") as String
        val time = get("time") as String
        val hours = get("hours") as Number
        val place = get("place") as String
        val category = get("category") as String
        val user = get("user") as String
        val accepted = get("accepted") as String
        TimeslotModel(id, title, description, date, time, hours, place, category, user, accepted)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

data class Profile(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val username: String = "",
    val email: String = "",
    val location: String = "",
    val description: String = "",
    val skills: List<String> = emptyList(),
    val image: Number = -1,
    val hours: Number = 0,
    val interested: List<String> = emptyList()
    )

fun DocumentSnapshot.toProfile(): Profile? {
    return try {
        val name = get("name") as String
        val surname = get("surname") as String
        val username = get("username") as String
        val email = get("email") as String
        val location = get("location") as String
        val description = get("description") as String
        val skills = get("skills") as List<String>
        val image = get("image") as Number
        val interested = get("interested") as List<String>
        val hours = get("hours") as Number
        Profile(
            id, name, surname, username, email, location, description, skills, image, hours, interested
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

data class Message(
    var user: Boolean = false,
    var content: String = "",
    var date: Long = 0,
    var offer: Boolean = false
)

data class ChatM(
    var id: String = "",
    var messages: List<Map<String, *>> = emptyList(),
    var replies: List<Map<String, *>> = emptyList(),
    var user: String = "",
    var interested: String = "",
    var timeslot: String = ""
)

fun DocumentSnapshot.toChatM(): ChatM? {
    return try {
        val messages = get("messages") as List<Map<String, *>>
        val replies = get("replies") as List<Map<String, *>>
        val user = get("user") as String
        val interested = get("interested") as String
        val timeslot = get("timeslot") as String
        ChatM(id, messages, replies, user, interested, timeslot)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

//REVIEWS SECTION

data class Review(
    var id: String,
    var rating: Number,
    var text: String,
    var receiverId: String,
    var timeslotId: String,
    var senderId: String,
    var senderUsername: String?,
    var senderProfileImage: Bitmap?,
    var date: String,
    var asWorker: Boolean,  //AsWorker -> 1 - AsUser/Requester -> 0
)

//To get data as reviews (senderUsername and senderProfileImage must be got trough profileInfo
fun DocumentSnapshot.toReview(): Review? {
    return try {
        val rating = get("rating") as Number
        val date = get("date") as String
        val receiverId = get("receiverId") as String
        val text = get("text") as String
        val timeslotId = get("timeslotId") as String
        val senderId = get("senderId") as String
        val asWorker = get("asWorker") as Boolean

        Review(
            id,
            rating,
            text,
            receiverId,
            timeslotId,
            senderId,
            null,
            null,
            date,
            asWorker
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

