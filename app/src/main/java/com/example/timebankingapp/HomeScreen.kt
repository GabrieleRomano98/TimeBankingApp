package com.example.timebankingapp

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.math.abs


class HomeScreen : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var myAdapter: RecyclerView.Adapter<*>

    data class FakeTimeslot(
        var title: String,
        var description: String,
        var date: String,
        var place: String,
        var time: String,
        var hours: Int,
        var category: String,
        var id: String,
        var rating: Float,
        var imageUrl: String
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Is user authenticated?
        if (Firebase.auth.currentUser == null) {
            findNavController().navigate(R.id.action_home_to_googleSignInFragment2)
        }

        val orientation = this.resources.configuration.orientation
        val sCount = if (orientation == Configuration.ORIENTATION_PORTRAIT) 3 else 5

        recyclerView = view.findViewById(R.id.categories_recycler_view)
        val gridLayoutManager =
            GridLayoutManager(context, sCount)
        recyclerView.layoutManager = gridLayoutManager

        myAdapter = CategoriesAdapter(mutableListOf(
            "Caregiving", "Gardening", "Housework", "Handwork", "Pet caring", "Technology", "Other")) {
            adapterOnClick(it)
        }

        recyclerView.adapter = myAdapter


    }

    private fun adapterOnClick(jobCategory: String) {
        Log.d("JobCategory", jobCategory)
        val bundle = bundleOf("Category" to jobCategory)

        findNavController().navigate(R.id.action_home_to_timeslots, bundle)
    }


}

