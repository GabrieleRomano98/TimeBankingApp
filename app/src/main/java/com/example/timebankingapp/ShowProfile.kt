package com.example.timebankingapp

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Button
import android.widget.RatingBar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar

class ShowProfile : Fragment() {
    private val vm: MyViewModel by activityViewModels()

    private var userId: String? = null

    private var ratingAsWorker: Float = 0.0f
    private var nOfReviewAsWorker: Int = 0
    private var ratingAsUser: Float = 0.0f
    private var nOfReviewAsUser: Int = 0

    private var visibility = true

    private lateinit var mySnackbarNoReviews: Snackbar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_show_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Set snackbar when there are no reviews to check
        mySnackbarNoReviews = Snackbar.make(view, R.string.SnackBar_NoReviews, Snackbar.LENGTH_SHORT)


        view.findViewById<TextView>(R.id.reviewsAmountAsUser).text =
            String.format(resources.getString(R.string.nOfRatings), "0")
        view.findViewById<TextView>(R.id.reviewsAmountAsWorker).text =
            String.format(resources.getString(R.string.nOfRatings), "0")

        if (arguments == null) {

            setHasOptionsMenu(true)

            vm.userAuth.observe(viewLifecycleOwner) { user ->
                if (user != null) { // if there is no profile data static information is used
                    fillFields(view, user)
                    reviewsObserve(user, view)
                }
                vm.imageAuth.observe(viewLifecycleOwner) {
                    view.findViewById<ShapeableImageView>(R.id.profileImage).setImageBitmap(it)
                }

            }
            if (vm.userAuth.value != null)
                userId = vm.userAuth.value?.id

        } else {
            visibility = false
            requireArguments().getString("userId")?.let { it ->
                vm.getProfileById(it).observe(viewLifecycleOwner) { user ->
                    if (user != null) {
                        fillFields(view, user)

                    }
                    reviewsObserve(user, view)
                }
                vm.getProfileImage(it).observe(viewLifecycleOwner) { image ->
                    if (image != null) {
                        view.findViewById<ShapeableImageView>(R.id.profileImage)
                            .setImageBitmap(image)
                    }
                }
                userId = it
            }

        }
        view.findViewById<Button>(R.id.reviewsButtonAsUser).setOnClickListener {
            mySnackbarNoReviews.show()
        }
        view.findViewById<Button>(R.id.reviewsButtonAsWorker).setOnClickListener {
            mySnackbarNoReviews.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.edit_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.title == "edit")
            findNavController().navigate(R.id.action_showProfile_to_editProfile)
        return super.onOptionsItemSelected(item)
    }


    private fun reviewsObserve(user: Profile, view: View) {
        vm.getReviews(user.id, false).observe(viewLifecycleOwner) { reviews ->
            if (reviews?.isEmpty() == false){
                view.findViewById<Button>(R.id.reviewsButtonAsUser).setOnClickListener{
                    val bundle = bundleOf("userId" to userId)
                    bundle.putBoolean("asWorker", false)
                    bundle.putFloat("rating", ratingAsUser)
                    bundle.putInt("nOfReviews", nOfReviewAsUser )
                    findNavController().navigate(R.id.action_showProfile_to_fragment_reviewsList, bundle)
                }
            }else{
                mySnackbarNoReviews.show()
            }
            fillRatingsFields(view, reviews, false)
        }
        vm.getReviews(user.id, true).observe(viewLifecycleOwner) { reviews ->
            if (reviews?.isEmpty() == false){
                view.findViewById<Button>(R.id.reviewsButtonAsWorker).setOnClickListener {
                    val bundle = bundleOf("userId" to userId)
                    bundle.putBoolean("asWorker", true)
                    bundle.putFloat("rating", ratingAsWorker)
                    bundle.putInt("nOfReviews", nOfReviewAsWorker )
                    findNavController().navigate(R.id.action_showProfile_to_fragment_reviewsList, bundle)
                }
            }else{
                mySnackbarNoReviews.show()
            }
            fillRatingsFields(view, reviews, true)
        }
    }

    private fun fillRatingsFields(view: View, reviews: MutableList<Review>?, asWorker: Boolean) {
        val rating = if (reviews != null) {
            val ratings: List<Float> = reviews.map { it.rating.toFloat() }
            ratings.average().toFloat()
        } else {
            0.0f
        }
        val nOfReviews = reviews?.count() ?: 0

        if (asWorker) {
            ratingAsWorker = rating
            nOfReviewAsWorker = nOfReviews
            view.findViewById<TextView>(R.id.ratingValueAsWorker).text = rating.toString()
            view.findViewById<RatingBar>(R.id.ratingBarAsWorker).rating = rating
            view.findViewById<TextView>(R.id.reviewsAmountAsWorker).text =
                String.format(resources.getString(R.string.nOfRatings), nOfReviews.toString())

        } else {
            ratingAsUser = rating
            nOfReviewAsUser = nOfReviews
            view.findViewById<TextView>(R.id.ratingValueAsUser).text = rating.toString()
            view.findViewById<RatingBar>(R.id.ratingBarAsUser).rating = rating
            view.findViewById<TextView>(R.id.reviewsAmountAsUser).text =
                String.format(resources.getString(R.string.nOfRatings), nOfReviews.toString())
        }
    }

    private fun fillFields(view: View, user: Profile) {
        view.findViewById<TextView>(R.id.name).text = user.name
        view.findViewById<TextView>(R.id.surname).text = user.surname
        view.findViewById<TextView>(R.id.username).text = user.username
        view.findViewById<TextView>(R.id.email).text = user.email
        view.findViewById<TextView>(R.id.location).text = user.location
        view.findViewById<TextView>(R.id.description).text = user.description
        if(visibility)
            view.findViewById<TextView>(R.id.hours).text = "${user.hours} Hours"
        else
            view.findViewById<TextView>(R.id.hours).visibility = View.GONE
        val cg = view.findViewById<ChipGroup>(R.id.skills)
        cg.removeAllViews()
        user.skills.forEach { t ->
            val x = Chip(context)
            x.text = t
            cg.addView(x)
        }
    }


}