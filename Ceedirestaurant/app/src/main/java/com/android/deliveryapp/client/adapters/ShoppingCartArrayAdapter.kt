package com.android.deliveryapp.client.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.android.deliveryapp.R
import com.android.deliveryapp.util.ProductItem
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ShoppingCartArrayAdapter(
        private val activity: Activity,
        layout: Int,
        private val array: Array<ProductItem>
) : ArrayAdapter<ProductItem>(activity, layout, array) {

    internal class ViewHolder {
        var title: TextView? = null
        var price: TextView? = null
        var quantity: TextView? = null
        var type: TextView?=null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?

        if (convertView == null) {
            view = activity.layoutInflater.inflate(R.layout.list_element_shopping_cart, null)

            val viewHolder = ViewHolder()
            viewHolder.title = view.findViewById(R.id.productName)
            viewHolder.price = view.findViewById(R.id.productPrice)
            viewHolder.quantity = view.findViewById(R.id.productQty)
            viewHolder.type = view.findViewById(R.id.type1)
            view.tag = viewHolder
        } else {
            view = convertView
        }

        val holder = view?.tag as ViewHolder
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/init",
            Response.Listener { response ->


                try {

                    holder.type?.text =  "Type  ${extractJSON(response)}"
                    Toast.makeText(activity, "on_init", Toast.LENGTH_LONG).show()

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
                params2.put("use_case", "on_init/sending_the_final_quote_and_payment_terms")
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

        holder.title?.text = array[position].title
        holder.price?.text = String.format("%.2f ₹", array[position].price)
        holder.quantity?.text = array[position].quantity.toString()

        return view
    }
    private fun extractJSON(response:String): String {
        val jsonArray = JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("order")
        val state = order.getJSONArray("items")
        val array1 = state.getJSONObject(0)
        val id = array1.get("id")
        println(id)

        return id.toString()

    }
}