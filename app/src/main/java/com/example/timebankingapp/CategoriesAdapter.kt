package com.example.timebankingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class CategoriesAdapter(
    private var CategoriesList: MutableList<String>,
    private val onClickAdapter: (String) -> Unit
) : RecyclerView.Adapter<CategoriesAdapter.CategoriesViewHolder>() {


    class CategoriesViewHolder(view: View, onClickAdapter: (String) -> Unit) :
        RecyclerView.ViewHolder(view) {
        private val categoryName: TextView = view.findViewById(R.id.category_name)
        private val categoryImage: ImageView = view.findViewById(R.id.category_image)
        private var currentCategory: String = ""

        init {
            view.setOnClickListener{ onClickAdapter(categoryName.text.toString()) }
        }

        fun bind(text: String) {
            currentCategory = text

            categoryName.text = currentCategory
            when (currentCategory) {
                "Caregiving" -> {
                    categoryImage.setBackgroundResource(R.drawable.caregiving_job_icon)
                }
                "Gardening" -> {
                    categoryImage.setBackgroundResource(R.drawable.gardening_job_icon)
                }
                "Pet caring" -> {
                    categoryImage.setBackgroundResource(R.drawable.petcaring_job_icon)
                }
                "Housework" -> {
                    categoryImage.setBackgroundResource(R.drawable.housework_job_icon)
                }
                "Handwork" -> {
                    categoryImage.setBackgroundResource(R.drawable.handwork_job_icon)
                }
                "Technology" -> {
                    categoryImage.setBackgroundResource(R.drawable.technology_job_icon)
                }
                else -> {
                    categoryImage.setBackgroundResource(R.drawable.othercategory_job_icon)
                }
            }

        }

        fun unbind(view: View) {
            view.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriesViewHolder {
        val vg = LayoutInflater.from(parent.context)
            .inflate(R.layout.categories_item, parent, false)
        return CategoriesViewHolder(vg,onClickAdapter)
    }

    override fun onBindViewHolder(holder: CategoriesViewHolder, position: Int) {
        val category = CategoriesList[position]
        holder.bind(category)
    }

    override fun onViewRecycled(holder: CategoriesViewHolder) {
        holder.unbind(holder.itemView)
    }

    override fun getItemCount(): Int = CategoriesList.size
}