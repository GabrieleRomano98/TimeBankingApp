package com.example.timebankingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator


class ReviewsAdapter(
    private var reviewsList: MutableList<Review>
) : RecyclerView.Adapter<ReviewsAdapter.ReviewsViewHolder>() {

    fun setReviewsList(newReviews: MutableList<Review>) {
        val diffs = DiffUtil.calculateDiff(
            ReviewsDiffCallback(reviewsList, newReviews)
        )
        reviewsList = newReviews
        diffs.dispatchUpdatesTo(this)
    }

    class ReviewsViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        private val reviewerName: TextView = view.findViewById(R.id.reviewerName)
        private val reviewerImage: ImageView = view.findViewById(R.id.reviewerProfileImage)
        private val reviewDate: TextView = view.findViewById(R.id.reviewDate)
        private val reviewRating: TextView = view.findViewById(R.id.reviewRating)
        private val reviewRatingBar: RatingBar = view.findViewById(R.id.reviewRatingBar)
        private val reviewDescription: TextView = view.findViewById(R.id.reviewDescription)

        private var currentReview: Review? = null


        fun bind(review: Review) {
            currentReview = review


            reviewerName.text = review.senderUsername
            reviewerImage.setImageBitmap(review.senderProfileImage)
            reviewDate.text = review.date
            reviewRating.text = review.rating.toString()
            reviewRatingBar.rating = review.rating.toFloat()
            reviewDescription.text = review.text


        }

        fun unbind(view: View) {
            view.setOnClickListener(null)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewsViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.reviews_list_item, parent, false)
        return ReviewsViewHolder(vg)
    }

    override fun onBindViewHolder(holder: ReviewsViewHolder, position: Int) {
        val review = reviewsList[position]
        holder.bind(review)
    }

    override fun onViewRecycled(holder: ReviewsViewHolder) {
        holder.unbind(holder.itemView)
    }

    override fun getItemCount(): Int = reviewsList.size


}

class ReviewsDiffCallback(private val rev: List<Review>, private val newRev: List<Review>) :
    DiffUtil.Callback() {
    override fun getOldListSize(): Int = rev.size
    override fun getNewListSize(): Int = newRev.size
    override fun areItemsTheSame(oldP: Int, newP: Int): Boolean {
        return (
                rev[oldP].id == newRev[newP].id &&
                        rev[oldP].rating == newRev[newP].rating &&
                        rev[oldP].date == newRev[newP].date &&
                        rev[oldP].text == newRev[newP].text &&
                        rev[oldP].receiverId == newRev[newP].receiverId &&
                        rev[oldP].timeslotId == newRev[newP].timeslotId &&
                        rev[oldP].senderId == newRev[newP].senderId &&
                        rev[oldP].senderUsername == newRev[newP].senderUsername &&
                        rev[oldP].asWorker == newRev[newP].asWorker
                //rev[oldP].image == newRev[newP].image
                )
    }

    override fun areContentsTheSame(oldP: Int, newP: Int): Boolean {
        return rev[oldP].id == newRev[newP].id
    }
}


class HeaderReviewsAdapter(
    private var rating: Float,
    private var nOfReviews: Int,
    private var percentages: IntArray
) :
    RecyclerView.Adapter<HeaderReviewsAdapter.HeaderReviewsViewHolder>() {

    fun setRating(newRating: Float, newNOfReviews: Int, newPercentages: IntArray) {
        rating = newRating
        nOfReviews = newNOfReviews
        percentages = newPercentages
        notifyItemChanged(0)
    }

    class HeaderReviewsViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val ratingValue: TextView = view
            .findViewById(R.id.ratingValueHeaderReviews)
        private val ratingBar: RatingBar = view
            .findViewById(R.id.ratingBarHeaderReviews)
        private val nOfReviewsText: TextView = view
            .findViewById(R.id.nOfRatingsHeaderReviews)
        private val percentage01: LinearProgressIndicator = view
            .findViewById(R.id.linearProgressIndicator01)
        private val percentage12: LinearProgressIndicator = view
            .findViewById(R.id.linearProgressIndicator12)
        private val percentage23: LinearProgressIndicator = view
            .findViewById(R.id.linearProgressIndicator23)
        private val percentage34: LinearProgressIndicator = view
            .findViewById(R.id.linearProgressIndicator34)
        private val percentage45: LinearProgressIndicator = view
            .findViewById(R.id.linearProgressIndicator45)

        fun bind(rating: Float, nOfReviews: Int, percentages: IntArray) {
            ratingValue.text = rating.toString()
            ratingBar.rating = rating
            nOfReviewsText.text =
                String.format(view.resources.getString(R.string.nOfRatings), nOfReviews.toString())

            updateProgress(percentage01,percentages[0])
            updateProgress(percentage12,percentages[1])
            updateProgress(percentage23,percentages[2])
            updateProgress(percentage34,percentages[3])
            updateProgress(percentage45,percentages[4])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderReviewsViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.reviews_list_adapter_header, parent, false)
        return HeaderReviewsViewHolder(vg)
    }

    override fun onBindViewHolder(viewHolder: HeaderReviewsViewHolder, position: Int) {
        viewHolder.bind(rating, nOfReviews, percentages)
    }

    override fun getItemCount() = 1

}

private fun updateProgress(viewProgressIndicator: LinearProgressIndicator,progress: Int) {
    val color = when (progress) {
        in 0..19 -> R.color.red
        in 20..39 -> R.color.orange_red
        in 40..59 -> R.color.yellow
        in 60..79 -> R.color.lawn_green
        else -> R.color.dark_green
    }
    updateProgressBar(progress, color, viewProgressIndicator)
}

private fun updateProgressBar(progress: Int, color: Int, view: LinearProgressIndicator) {
    view.apply {
        setProgress(progress)
        setIndicatorColor(resources.getColor(color))
    }
}
