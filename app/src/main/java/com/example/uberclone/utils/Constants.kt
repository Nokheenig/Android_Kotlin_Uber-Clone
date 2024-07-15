package com.example.uberclone.utils

import android.util.Log
import com.example.uberclone.models.DriverInfoModel

object Constants {
    const val DRIVER_INFO_REFERENCE = "DriverInfo"
    //const val RC_SIGN_IN = 123
    const val DATABASE_URL = "https://uber-clone-d6067-default-rtdb.europe-west1.firebasedatabase.app"
    var currentUser: DriverInfoModel? = null
    const val RIDERS_LOCATION_REFERENCE = "DriversLocation"
    fun log(className: String, methodName: String, message: String) {
        Log.d("testDebug", "[$className-$methodName] - $message")
    }

    fun buildWelcomeMessage():String {
        return StringBuilder("Welcome, ")
            .append(currentUser?.firstName)
            .append("")
            .append(currentUser?.lastName)
            .toString()
    }
}