package com.xeatpos.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.res.ResourcesCompat
import com.example.receipeapp2.R

class CustomProgressDialog {
    lateinit var dialog: CustomDialog

    fun show(context: Context): Dialog {
        return show(context, null)
    }

    fun show(context: Context, title: CharSequence?): Dialog {
        val inflater = (context as Activity).layoutInflater
        val view = inflater.inflate(R.layout.dialog_progressbar, null)
        var titleee = view.findViewById<TextView>(R.id.cp_title)

        if (title != null) {
            titleee.setText(title.toString())
        }

        // Card Color
        var cp_cardview = view.findViewById<CardView>(R.id.cp_cardview)
        cp_cardview.setCardBackgroundColor(Color.parseColor("#70000000"))

        // Progress Bar Color
        var cp_progressBar = view.findViewById<ProgressBar>(R.id.cp_pbar)
        setColorFilter(cp_progressBar.indeterminateDrawable, ResourcesCompat.getColor(context.resources, R.color.green, null))

        // Text Color
        titleee.setTextColor(Color.WHITE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            dialog = CustomDialog(context)
        }
        dialog.setContentView(view)
        dialog.show()
        return dialog
    }

    private fun setColorFilter(drawable: Drawable, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        } else {
            @Suppress("DEPRECATION")
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT_WATCH)
    class CustomDialog(context: Context) : Dialog(context, R.style.CustomDialogTheme) {
        init {
            // Set Semi-Transparent Color for Dialog Background
            window?.decorView?.rootView?.setBackgroundResource(R.color.blue)
            window?.decorView?.setOnApplyWindowInsetsListener { _, insets ->
                insets.consumeSystemWindowInsets()
            }
        }
    }
}