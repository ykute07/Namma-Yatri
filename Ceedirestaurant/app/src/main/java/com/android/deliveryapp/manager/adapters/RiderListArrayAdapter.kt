package com.android.deliveryapp.manager.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.android.deliveryapp.R
import com.android.deliveryapp.util.RiderListItem

class RiderListArrayAdapter(
        private val activity: Activity,
        layout: Int,
        private val array: Array<RiderListItem>
) : ArrayAdapter<RiderListItem>(activity, layout, array) {

    internal class ViewHolder {
        var email: TextView? = null
        var availability: ImageView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?

        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.manager_rider_list_element, null)

            val viewHolder = ViewHolder()
            viewHolder.email = view.findViewById(R.id.riderEmail)
            viewHolder.availability = view.findViewById(R.id.riderAvailability)
            view.tag = viewHolder
        } else {
            view = convertView
        }

        val holder = view?.tag as ViewHolder
        holder.email?.text = array[position].email

        if (array[position].availability) { // if is available
            holder.availability?.load(R.drawable.accept) {
                crossfade(true)
            }
        } else { // not available
            holder.availability?.load(R.drawable.cancel) {
                crossfade(true)
            }
        }

        return view
    }
}