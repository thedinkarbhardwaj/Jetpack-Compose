package com.example.receipeapp2

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receipeapp2.Signup.SignupModel
import com.example.receipeapp2.Utils.Utility
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.throneride.apiServices.ApiClient
import com.xeatpos.utils.CustomProgressDialog
import itemadpt
import retrofit2.Call
import retrofit2.Response

class MainViewModel: ViewModel() {

    lateinit var act: MainActivity
    private val progressDialog = CustomProgressDialog()

    lateinit var adpt: itemadpt
    var token = ""
    var itemResp= MutableLiveData<CategoryResponse>()

    var itemlist= mutableListOf<CategoryResponse>();


    fun init(activity: MainActivity) {
        act = activity

        apiItemList()




    }

    fun apiItemList() {

        itemlist.clear()

        getItemResp()
    }


    fun getItemResp() {
        if (Utility.isNetworkConnected(act)) {
            progressDialog.show(act, "Please wait...")

            val result = ApiClient.api().category()

            result.enqueue(object : retrofit2.Callback<List<CategoryResponse>> {
                override fun onResponse(call: Call<List<CategoryResponse>>, response: Response<List<CategoryResponse>>) {
                    progressDialog.dialog.dismiss()

                    if (response.isSuccessful) {
                        response.body()?.let { categories ->
                            // Clear previous items if needed
                            itemlist.clear()
                            itemlist.addAll(categories) // Assuming itemlist is a List<CategoryResponse>
                            recItem() // Update the RecyclerView
                        } ?: run {
                            Toast.makeText(act, act.getString(R.string.tryagain), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(act, act.getString(R.string.tryagain), Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<CategoryResponse>>, t: Throwable) {
                    progressDialog.dialog.dismiss()
                    Log.e("API Error", t.message.orEmpty())
                    Toast.makeText(act, act.getString(R.string.tryagain), Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(act, act.getString(R.string.err_internet_connection), Toast.LENGTH_SHORT).show()
        }
    }





    fun recItem() {

        var notirec = act.binding.recItem
        val mLayoutManagerGrpClas = LinearLayoutManager(act)
        notirec.layoutManager = mLayoutManagerGrpClas
        notirec.itemAnimator = DefaultItemAnimator()

        adpt = itemadpt(act, itemlist)
        notirec.adapter = adpt


    }

}
