package com.example.receipeapp2.Utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import com.example.receipeapp2.R
import com.tapadoo.alerter.Alerter
import java.util.Locale

object Utility {



    fun isValidEmail(email: String): Boolean {
        // Implement your email validation logic here
        // For a simple email format check, you can use a regular expression
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }

    fun alertError(context: Activity, message: String?) {
        Alerter.create(context)
            .setText(message.toString())
            .setIcon(R.drawable.ic_error_icon)
            .setBackgroundColorRes(R.color.red) // or setBackgroundColorInt(Color.CYAN)
            .show()
    }

    fun successAlert(context: Activity, message: String?) {
        Alerter.create(context)
            .setText(message.toString())
            .setIcon(R.drawable.ic_done_icon)
            .setBackgroundColorRes(R.color.green) // or setBackgroundColorInt(Color.CYAN)
            .show()
    }

    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (cm != null) {
            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }
        return false
    }

}
