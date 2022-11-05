package com.example.timebankingapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

class ReviewsList : Fragment() {

    private val vm: MyViewModel by activityViewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: ReviewsAdapter
    private lateinit var myAdapterHeader: HeaderReviewsAdapter

    private var asWorker: Boolean = false
    private lateinit var userID: String
    private var rating: Float = 0f
    private var nOfReviews: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_reviews_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Message when there are no reviews items
        val sb = Snackbar.make(
            view,
            R.string.SnackBar_NoReviewsItems,
            BaseTransientBottomBar.LENGTH_SHORT
        )


        if (arguments != null) {
            //Get asUser/asWorker flag to show reviews
            asWorker = requireArguments().getBoolean("asWorker", true)
            userID = requireArguments().getString("userId", "")
            rating = requireArguments().getFloat("rating", 0f)
            nOfReviews = requireArguments().getInt("nOfReviews", 0)
        }
        Log.d("UserID:", userID)


        //Get the reviewsList
        vm.getReviews(userID, asWorker).observe(viewLifecycleOwner) { reviews ->

            val rList = mutableListOf<Review>()
            reviews?.forEach { review ->
                Log.d("ReviewObserve", review.toString())
                rList.add(review)
            }

            //Check if score has been changed
            val rating = if (reviews != null) {
                val ratings: List<Float> = reviews.map { it.rating.toFloat() }
                ratings.average().toFloat()
            } else {
                0.0f
            }
            val nOfReviews = reviews?.count() ?: 0
            val range01 = 0f..0.99f
            val percentage01 = reviews?.count {
                Log.d("Rating between 4 and 5", range01.contains(it.rating.toFloat()).toString())
                range01.contains(it.rating.toFloat())
            }?.times(100)?.div(nOfReviews) ?: 0
            val range12 = 1f..1.99f
            val percentage12 = reviews?.count {
                Log.d("Rating between 4 and 5", range12.contains(it.rating.toFloat()).toString())
                range12.contains(it.rating.toFloat())
            }?.times(100)?.div(nOfReviews) ?: 0
            val range23 = 2f..2.99f
            val percentage23 = reviews?.count {
                Log.d("Rating between 4 and 5", range23.contains(it.rating.toFloat()).toString())
                range23.contains(it.rating.toFloat())
            }?.times(100)?.div(nOfReviews) ?: 0
            val range34 = 3f..3.99f
            val percentage34 = reviews?.count {
                Log.d("Rating between 4 and 5", range34.contains(it.rating.toFloat()).toString())
                range34.contains(it.rating.toFloat())
            }?.times(100)?.div(nOfReviews) ?: 0
            val range45 = 4f..5f
            val percentage45 = reviews?.count {
                Log.d("Rating between 4 and 5", range45.contains(it.rating.toFloat()).toString())
                range45.contains(it.rating.toFloat())
            }?.times(100)?.div(nOfReviews) ?: 0

            Log.d(
                "percentage",
                "$percentage01 $percentage12 $percentage23 $percentage34 $percentage45"
            )
            val percentages = intArrayOf(percentage01,percentage12,percentage23,percentage34,percentage45)

            myAdapterHeader.setRating(rating, nOfReviews, percentages)


            //Checking how many items there are in the list
            if (rList.isEmpty()) {
                sb.show()
            } else {
                myAdapter.setReviewsList(rList)
            }
        }
        recyclerView = view.findViewById(R.id.reviewsList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        myAdapter = ReviewsAdapter(
            mutableListOf()
        )
        myAdapterHeader = HeaderReviewsAdapter(rating, nOfReviews, intArrayOf(0,0,0,0,0))
        recyclerView.adapter = ConcatAdapter(myAdapterHeader, myAdapter)
    }

}
