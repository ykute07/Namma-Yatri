package com.android.deliveryapp.util

class Keys {
    companion object {
        const val users: String = "users"

        // user types
        const val CLIENT: String = "CLIENT"
        const val RIDER: String = "RIDER"
        const val MANAGER: String = "MANAGER"

        // general user preferences
        const val userInfo: String = "userInfo"
        const val userType: String = "userType"
        const val isLogged: String = "isLogged"
        const val isRegistered: String = "isRegistered"
        const val hasLocation: String = "hasLocation"

        // if is logged
        const val username: String = "username"
        const val pwd: String = "password"
        const val invalidUser = "invalidUser"

        // manager preferences
        const val managerPref = "managerPref"
        const val clientEmail = "clientEmail"
        const val riderEmail = "riderEmail"
        const val orderDate = "orderDate"

        // firebase string keys
        const val clients: String = "clients"
        const val manager: String = "manager"
        const val riders: String = "riders"
        const val  reviewRider : String ="review"

        const val riderPosition = "riderPosition"

        const val delivery: String = "delivery"
        const val deliveryHistory: String = "deliveryHistory"

        const val newDelivery: String = "newDelivery"

        const val YET_TO_RESPOND: String = "YET_TO_RESPOND"
        const val YET_TO_RESPOND_BY_RIDER :String = "YET_TO_RESPOND_BY_RIDER"
        const val ACCEPTED: String = "ACCEPTED"
        const val START: String = "START"
        const val DELIVERED: String = "DELIVERED"
        const val DELIVERY_FAILED: String = "DELIVERY_FAILED"
        const val REJECTED: String = "REJECTED"

        const val themePref = "themePref"

        const val chatCollection: String = "chats"

        const val riderStatus: String = "riderStatus"

        const val clientAddress: String = "address"
        const val shoppingCart: String = "shoppingCart"

        const val orders: String = "orders"
        const val reviewsRestaurant: String = "reviewsRestaurant"
        const val review : String ="review"
        const val reviewsRider: String = "reviewRiders"

        const val marketPosFirestore: String = "marketPos"
        const val marketDocument: String = "MARKET"
        const val fieldPosition: String = "position"

        const val productListFirebase: String = "productList"
        const val productImages: String = "productImages"

        const val name: String = "name"
        const val price: String = "price"
    }
}