package com.ksquare.shoesui.Model

import androidx.annotation.DrawableRes

    data class ProductData(
        val id:String,
        var name:String,
        var color:Int,
        var price:String,
        var discount:Float,
        var size:Int,
        var rating:Float,
        @DrawableRes var image:Int  // @DrawableRes annotation means it come from drawable only
    )