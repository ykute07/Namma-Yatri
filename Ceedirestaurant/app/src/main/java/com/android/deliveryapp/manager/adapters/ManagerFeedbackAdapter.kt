package com.android.deliveryapp.manager.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.android.deliveryapp.R
import com.android.deliveryapp.util.FeedbackReviewItem

class ManagerFeedbackAdapter (
    private val activity: Activity,
    layout: Int,
    private val array: Array<FeedbackReviewItem>
) : ArrayAdapter<FeedbackReviewItem>(activity, layout, array) {

    internal class ViewHolder {
        var review: TextView? = null

    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?

        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.manager_feedback_list_element, null)

            val viewHolder = ViewHolder()
            viewHolder.review = view.findViewById(R.id.reviewManager)

            view.tag = viewHolder
        } else {
            view = convertView
        }

        val holder = view?.tag as ViewHolder
        holder.review?.text = array[position].review



        return view
    }
}