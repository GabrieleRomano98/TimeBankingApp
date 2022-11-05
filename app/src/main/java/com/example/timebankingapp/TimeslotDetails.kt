package com.example.timebankingapp

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class TimeslotDetails : Fragment(), SendReviewFragment.SendReviewListener {

    private lateinit var favBtn: ShapeableImageView
    private val vm: MyViewModel by activityViewModels()
    private lateinit var timeslotId: String
    private var favorite = false

    private lateinit var ts: TimeslotModel
    private var mode: Boolean = false

    private lateinit var mySnackError: Snackbar
    private lateinit var mySnackSent: Snackbar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_timeslot_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mySnackError = Snackbar.make(view, R.string.SnackBar_NoRating, Snackbar.LENGTH_SHORT)
        mySnackSent = Snackbar.make(view, R.string.SnackBar_ReviewSent, Snackbar.LENGTH_SHORT)
        val reviewButton = view.findViewById<Button>(R.id.timeslot_details_reviewButton)

        vm.timeslots.observe(viewLifecycleOwner) { list ->

            ts = list.find { requireArguments().getString("id") == it.id } ?: TimeslotModel()
            timeslotId = ts.id
            val hours = ts.hours.toString().plus(" Hours")
            view.findViewById<TextView>(R.id.timeslot_details_title).text = ts.title
            view.findViewById<Button>(R.id.buttonChat).setOnClickListener {
                val b = bundleOf(
                    "timeslot" to ts.id,
                    "owner" to ts.user,
                    "user" to Firebase.auth.currentUser!!.uid
                )
                findNavController().navigate(R.id.action_timeslotDetails_to_chat, b)
            }
           //Check if he come from Accepted/Assigned Fragment
            if (ts.accepted == "") {
                reviewButton.visibility = View.GONE
            }
            else {
                //Check no reviews has been sent before
                mode = ts.accepted != Firebase.auth.currentUser!!.uid
                vm.getAllReviews().observe(viewLifecycleOwner) {
                    if (checkNoReviewsBefore(
                            Firebase.auth.currentUser!!.uid,
                            ts.user,
                            ts.id,
                            false,
                            it
                        )
                    ) {
                        reviewButton.setOnClickListener {
                            sendReview()
                        }
                    } else {
                        reviewButton.isEnabled = false
                    }
                }
            }
            view.findViewById<TextView>(R.id.timeslot_details_date).text = ts.date
            view.findViewById<TextView>(R.id.timeslot_details_time).text = ts.time
            view.findViewById<TextView>(R.id.timeslot_details_hours).text = hours
            view.findViewById<TextView>(R.id.timeslot_details_location).text = ts.place
            view.findViewById<TextView>(R.id.timeslot_details_description).text = ts.description
            view.findViewById<TextView>(R.id.timeslot_details_category).text = ts.category

            ///////Init user section
            vm.getProfileById(ts.user).observe(viewLifecycleOwner) {
                view.findViewById<TextView>(R.id.timeslot_details_profileName).text = it.username
                vm.getProfileImage(it.id).observe(viewLifecycleOwner){ img ->
                    if (it != null) {
                        view.findViewById<ShapeableImageView>(R.id.timeslot_profile_editbutton).setImageBitmap(img)
                    }
                }
            }
            view.findViewById<CardView>(R.id.card_view_profile_section).setOnClickListener{
                val bundle = bundleOf("userId" to ts.user )
                findNavController().navigate(R.id.action_timeslotDetails_to_showProfile2, bundle)
            }
            ///////get timeslot image
            vm.getTimeslotImage(ts.id).observe(viewLifecycleOwner) {
                view.findViewById<ImageView>(R.id.imageView2).setImageBitmap(it)
            }
        }

        favBtn = view.findViewById(R.id.favBtn)

        favBtn.setOnClickListener {
            if (!favorite)
                vm.addFavorite(timeslotId)
            else
                vm.removeFavorite(timeslotId)
        }

        vm.userAuth.observe(viewLifecycleOwner){
            if(it != null)
                if(it.interested.contains(timeslotId)){
                    favorite = true
                    favBtn.setImageResource(R.drawable.ic_baseline_favorite_24)
                } else {
                    favorite = false
                    favBtn.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                }
        }
    }

    private fun sendReview() {
        val reviewDialog = SendReviewFragment()
        val args = Bundle()
        args.putString("user", ts.user)
        args.putString("id", ts.id)
        args.putBoolean("mode", mode)

        reviewDialog.arguments = args

        reviewDialog.setListener(this)
        fragmentManager?.let { reviewDialog.show(it, "review") }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDialogPositiveClick(dialog: DialogFragment, rating: Float, text: String) {

        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("d LLL yyyy")
        val formatted = current.format(formatter).uppercase(Locale.getDefault())

        if (rating != 0.0f && vm.userAuth.value != null) {
            //Add review
            vm.addReview(
                rating,
                text,
                ts.user,
                ts.id,
                Firebase.auth.currentUser!!.uid,
                formatted,
                mode
            )
            mySnackSent.show()
        } else if (rating == 0.0f) {
            mySnackError.show()
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }


}

class SendReviewFragment : DialogFragment() {

    // Use this instance of the interface to deliver action events
    private lateinit var listener: SendReviewListener

    private lateinit var editText: EditText
    private lateinit var ratingBar: RatingBar

    /* The activity that creates an instance of this dialog fragment must
       * implement this interface in order to receive event callbacks.
       * Each method passes the DialogFragment in case the host needs to query it. */
    interface SendReviewListener : Serializable {
        fun onDialogPositiveClick(dialog: DialogFragment, rating: Float, text: String)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    fun setListener(_listener: SendReviewListener) {
        listener = _listener
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_review, null) as View

            ratingBar = view.findViewById(R.id.ratingBar2)
            editText = view.findViewById(R.id.editTextTextMultiLine)

            if(savedInstanceState != null)
                restoreData(savedInstanceState)

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.send) { _, _ ->
                    // Send the positive button event back to the host activity
                    val textToSend = if (editText.text != null) editText.text.toString() else ""
                    listener.onDialogPositiveClick(this, ratingBar.rating, textToSend)
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    // Send the negative button event back to the host activity
                    listener.onDialogNegativeClick(this)
                }

            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("description", (editText.text ?: "").toString())
        outState.putFloat("rating", (ratingBar.rating) )
        outState.putSerializable("listener", listener)    }

    private fun restoreData(b: Bundle) {
        editText.setText( b.getString("description", "").toString() )
        ratingBar.rating = b.getFloat("rating", 0.0f)
        listener = b.getSerializable("listener") as SendReviewListener
    }

}

fun checkNoReviewsBefore(
    senderId: String,
    receiverId: String,
    timeslotId: String,
    asWorker: Boolean,
    reviews: MutableList<Review>?
): Boolean {
    return reviews?.none {
        (it.timeslotId == timeslotId) &&
                (it.senderId == senderId) &&
                (it.receiverId == receiverId) &&
                (it.asWorker == asWorker)

    }
        ?: true
}

