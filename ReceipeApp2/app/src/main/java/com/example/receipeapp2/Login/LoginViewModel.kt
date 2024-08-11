package com.example.receipeapp2.Login

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.receipeapp2.R
import com.example.receipeapp2.Signup.SignupModel
import com.example.receipeapp2.Utils.Utility
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.throneride.apiServices.ApiClient
import com.xeatpos.utils.CustomProgressDialog
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response

class LoginViewModel: ViewModel() {

    lateinit var act: LoginAct

    private val progressDialog = CustomProgressDialog()
    var signinResp= MutableLiveData<SignupModel>()

    fun init(activity: LoginAct){
        act = activity

    }


    fun getLoginApi(){

        if (valid()){


            var map = HashMap<String, String>()
            map.put("email", act.binding.etEmail.text.toString())  //Email
            map.put("password", act.binding.etPass.text.toString())  //Password
            map.put("device_type", "1")  //0 for android 1 for ios
            map.put("device_token", "randommmTokennnnnnnnnn")  //0 for android 1 for ios

            getLoginResp(map)
        }

    }
    fun valid(): Boolean {

        var valid = false

        var email = act.binding.etEmail.text.trim().toString()
        var pass = act.binding.etPass.text?.trim().toString()


        if (email.isNullOrEmpty()){
            Utility.alertError(act, "Enter email")

        }
        else if (!Utility.isValidEmail(email.toString())) {
            Utility.alertError(
                act,"Correct email"
            )

        }
        else if (pass.isNullOrEmpty()) {
            Utility.alertError(act, "Enter pass")

        }
        else if (pass.length < 8) {
            Utility.alertError(act, ":Enter pass length minimum 8")

        }

        else {
            valid = true
        }


        return valid
    }

    fun getLoginResp(map: HashMap<String, String>){


        if (Utility.isNetworkConnected(act)){
            progressDialog.show(act, "Please wait...")

            var result =  ApiClient.api().login(map)

            result.enqueue(object :retrofit2.Callback<JsonObject>{
                override fun onResponse(call: Call<JsonObject>, data: Response<JsonObject>) {

                    if (data !=null){
                        progressDialog.dialog.dismiss()
                        val gson = Gson()
                        val signInModel: SignupModel = gson.fromJson(data.body(), SignupModel::class.java)

                        signinResp.postValue(signInModel)

                    }else{
                        progressDialog.dialog.dismiss()
                        Toast.makeText(act,act.getString(R.string.tryagain), Toast.LENGTH_SHORT).show()

                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(act,act.getString(R.string.tryagain),Toast.LENGTH_SHORT).show()


                    progressDialog.dialog.dismiss()

                }

            })
        }else{
            Toast.makeText(act,act.getString(R.string.err_internet_connection),Toast.LENGTH_SHORT).show()

        }


    }


}