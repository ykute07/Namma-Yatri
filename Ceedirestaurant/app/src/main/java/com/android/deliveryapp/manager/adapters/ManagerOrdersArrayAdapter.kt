package com.android.deliveryapp.manager.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.android.deliveryapp.R
import com.android.deliveryapp.util.ManagerOrderItem
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ManagerOrdersArrayAdapter(
        private val activity: Activity,
        layout: Int,
        private val array: Array<ManagerOrderItem>
) : ArrayAdapter<ManagerOrderItem>(activity, layout, array) {

    internal class ViewHolder {
        var email: TextView? = null
        var date: TextView? = null
        var total: TextView? = null
        var payment: TextView? = null
        var outcome: TextView? = null
        var statelabel :TextView?= null
        var typelabel:TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?

        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.manager_order_list_element, null)

            val viewHolder = ViewHolder()
            viewHolder.email = view.findViewById(R.id.orderEmail)
            viewHolder.date = view.findViewById(R.id.date)
            viewHolder.total = view.findViewById(R.id.total)
            viewHolder.payment = view.findViewById(R.id.payment)
            viewHolder.outcome = view.findViewById(R.id.outcome)
            viewHolder.statelabel = view.findViewById(R.id.statelabel)
            viewHolder.typelabel = view.findViewById(R.id.typelabel)

            view.tag = viewHolder
        } else {
            view = convertView
        }

        val holder = view?.tag as ViewHolder
        holder.email?.text = array[position].email
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/confirm",
            Response.Listener { response ->


                try {

                    holder.statelabel?.text = "${extractJSONfullfillment(response)}"
                    holder.typelabel?.text = "${extractJSON(response)}"
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
        holder.total?.text = String.format("₹ %.2f ", array[position].total)

        holder.payment?.text = array[position].payment
        holder.outcome?.text = array[position].outcome

        return view
    }

    private fun extractJSON(response:String): String {
        val jsonArray = JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("order")
        val state = order.get("state")
        val fullfillment = order.getJSONObject("fulfillment")
        val type = fullfillment.get("type")

        return state.toString()

    }
    private fun extractJSONfullfillment(response: String): String{
        val jsonArray = JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("order")
        val state = order.get("state")
        val fullfillment = order.getJSONObject("fulfillment")
        val type = fullfillment.get("type")

        return type.toString()
    }
}