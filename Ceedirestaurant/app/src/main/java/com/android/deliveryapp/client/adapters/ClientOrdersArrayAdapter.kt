package com.android.deliveryapp.client.adapters

import android.app.Activity
import android.content.ContentValues.TAG
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.android.deliveryapp.R
import com.android.deliveryapp.util.ClientOrderItem
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ClientOrdersArrayAdapter(
    private val activity: Activity,
    layout: Int,
    private val array: Array<ClientOrderItem>
) : ArrayAdapter<ClientOrderItem>(activity, layout, array) {

    internal class ViewHolder {
        var date: TextView? = null
        var totalPrice: TextView? = null
        var paymentType: TextView? = null
        var stateLabel:TextView?=null
        var typelabel:TextView?= null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?
        var state : String? =""


        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.list_element_order, null)

            val viewHolder = ViewHolder()
            viewHolder.date = view.findViewById(R.id.orderDate)
            viewHolder.totalPrice = view.findViewById(R.id.orderTotalPrice)
            viewHolder.paymentType = view.findViewById(R.id.paymentType)
            viewHolder.stateLabel=view.findViewById(R.id.statelabel)
            viewHolder.typelabel=view.findViewById(R.id.typelabel)
            view.tag = viewHolder
        } else {
            view = convertView
        }


        val holder = view?.tag as ViewHolder
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/confirm",
            Response.Listener { response ->


                try {
                    holder.stateLabel?.text =  "${extractJSON(response)}"
                    holder.typelabel?.text= "${extractJSONfullfillment(response)}"

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
            }) {
            override fun getBody(): ByteArray {
                val params2 = HashMap<String, String>()
                params2.put("domain","delivery" )
                params2.put("use_case", "on_confirm/confirmation_of_a_postpaid_order")
                params2.put("ttl", "10")
                params2.put("bpp_uri","http://13.235.139.60/sandbox/bpp1")
                params2.put("transaction_id","123871371289371983")
                return JSONObject(params2 as Map<*, *>).toString().toByteArray()
            }
            override fun getHeaders() : Map<String,String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"

                return params
            }

        }
        val requestQueue = Volley.newRequestQueue(activity)
        requestQueue.add(stringRequest)
        holder.date?.text = array[position].date
        holder.totalPrice?.text = String.format("%.2f ₹", array[position].totalPrice)
        holder.paymentType?.text = array[position].paymentType

        return view
    }

    private fun extractJSON(response:String): String {
        val jsonArray =JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("order")
        val state = order.get("state")
        val fullfillment = order.getJSONObject("fulfillment")
        val type = fullfillment.get("type")

        return state.toString()

    }
    private fun extractJSONfullfillment(response: String): String{
        val jsonArray =JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("order")
        val state = order.get("state")
        val fullfillment = order.getJSONObject("fulfillment")
        val type = fullfillment.get("type")

        return type.toString()
    }

}