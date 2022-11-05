package com.example.timebankingapp

import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MyTimeslotDetails : Fragment(), SendReviewFragment.SendReviewListener {

    private val vm: MyViewModel by activityViewModels()

    private lateinit var ts: TimeslotModel

    private lateinit var mySnackError: Snackbar
    private lateinit var mySnackSent: Snackbar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_timeslot_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        mySnackError = Snackbar.make(view, R.string.SnackBar_NoRating, Snackbar.LENGTH_SHORT)
        mySnackSent = Snackbar.make(view, R.string.SnackBar_ReviewSent, Snackbar.LENGTH_SHORT)
        val reviewButton = view.findViewById<Button>(R.id.myTimeslotDetails_reviewButton)


        vm.myTimeslots.observe(viewLifecycleOwner) { list ->
            ts = list.find { requireArguments().getString("id") == it.id } ?: TimeslotModel()
            val hours = ts.hours.toString().plus(" Hours")
            view.findViewById<TextView>(R.id.timeslot_details_title).text = ts.title
            view.findViewById<TextView>(R.id.timeslot_details_date).text = ts.date
            view.findViewById<TextView>(R.id.timeslot_details_time).text = ts.time
            view.findViewById<TextView>(R.id.timeslot_details_hours).text = hours
            view.findViewById<TextView>(R.id.timeslot_details_location).text = ts.place
            view.findViewById<TextView>(R.id.timeslot_details_description).text = ts.description
            view.findViewById<TextView>(R.id.timeslot_details_category).text = ts.category
            vm.getTimeslotImage(ts.id).observe(viewLifecycleOwner) {
                view.findViewById<ImageView>(R.id.imageView2).setImageBitmap(it)
            }
            if(ts.accepted == "") {
                setHasOptionsMenu(true)
            }
            else {
                reviewButton.visibility = View.VISIBLE
                reviewButton.visibility = View.VISIBLE
                //Check no reviews has been sent before
                vm.getAllReviews().observe(viewLifecycleOwner) {
                    if(checkNoReviewsBefore(Firebase.auth.currentUser!!.uid, ts.accepted,ts.id,true,it)) {
                        reviewButton.setOnClickListener {
                            sendReview()
                        }
                    }
                    else
                        reviewButton.isEnabled = false
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.title == "edit") {
            val bundle = bundleOf("id" to arguments?.getString("id"))
            findNavController().navigate(
                R.id.action_timeslotDetails_to_timeslotDetails_Edit,
                bundle
            )
        }
        return super.onOptionsItemSelected(item)
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
                ts.accepted,
                ts.id,
                Firebase.auth.currentUser!!.uid,
                formatted,
                true
            )
            mySnackSent.show()
        } else if (rating == 0.0f) {
            mySnackError.show()
        }
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }

    private fun sendReview() {
        val reviewDialog = SendReviewFragment()
        val args = Bundle()
        args.putString("user", ts.user)
        args.putString("id", ts.id)

        reviewDialog.arguments = args

        reviewDialog.setListener(this)
        fragmentManager?.let { reviewDialog.show(it, "review") }


    }
}