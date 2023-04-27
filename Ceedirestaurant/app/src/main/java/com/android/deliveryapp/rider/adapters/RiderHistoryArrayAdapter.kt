package com.android.deliveryapp.rider.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.android.deliveryapp.R
import com.android.deliveryapp.util.RiderHistoryItem
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RiderHistoryArrayAdapter(
    private val activity: Activity,
    layout: Int,
    private val array: Array<RiderHistoryItem>
) : ArrayAdapter<RiderHistoryItem>(activity, layout, array) {

    internal class ViewHolder {
        var date: TextView? = null
        var location: TextView? = null
        var outcome: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?

        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.rider_history_list_element, null)

            val viewHolder = ViewHolder()
            viewHolder.date = view.findViewById(R.id.dateHistory)
            viewHolder.location = view.findViewById(R.id.locationHistory)
            viewHolder.outcome = view.findViewById(R.id.outcomeHistory)
            view.tag = viewHolder
        } else {
            view = convertView
        }

        val holder = view?.tag as ViewHolder
        holder.date?.text = array[position].date
        holder.location?.text = array[position].location
        if( array[position].outcome =="DELIVERY_FAILED" ||array[position].outcome=="rejected"){
            val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/cancel",
                Response.Listener { response ->


                    try {
                        holder.outcome?.text =  "${extractJSON(response)}".toUpperCase()


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(activity, error.toString(), Toast.LENGTH_LONG).show()
                }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("domain","local-retail" )
                    params2.put("use_case", "on_cancel/cancellation_of_an_order_with_reason_for_cancellation")
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
        }
        else{
            holder.outcome?.text =  array[position].outcome
        }

        return view
    }

    private fun extractJSON(response:String): String {
        val jsonArray = JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("order")
        val state = order.get("state")


        return state.toString()

    }
}