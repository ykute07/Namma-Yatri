package com.android.deliveryapp.manager.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.android.deliveryapp.R
import com.android.deliveryapp.util.ProductItem

class OrderDetailAdapter(
    private val activity: Activity,
    layout: Int,
    private val array: Array<ProductItem>
) : ArrayAdapter<ProductItem>(activity, layout, array) {

    internal class ViewHolder {
        var title: TextView? = null
        var price: TextView? = null
        var quantity: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?

        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.manager_order_detail_list_element, null)

            val viewHolder = ViewHolder()
            viewHolder.title = view.findViewById(R.id.productNameDetail)
            viewHolder.price = view.findViewById(R.id.productPriceDetail)
            viewHolder.quantity = view.findViewById(R.id.productQtyDetail)
            view.tag = viewHolder
        } else {
            view = convertView
        }

        val holder = view?.tag as ViewHolder
        holder.title?.text = array[position].title
        holder.quantity?.text = array[position].quantity.toString()
        holder.price?.text = String.format("%.2f ₹", array[position].price)

        return view
    }
}