package com.example.receipeapp2.Signup

import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.receipeapp2.R
import com.example.receipeapp2.Utils.Utility
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.throneride.apiServices.ApiClient
import com.xeatpos.utils.CustomProgressDialog
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response

class SignupViewModel: ViewModel() {
    lateinit var act: Signup
    private val progressDialog = CustomProgressDialog()
    var signupResp= MutableLiveData<SignupModel>()


    fun init(activity: Signup) {
        act = activity

    }

    fun register() {

        if (valid()) {
            registerApi()

        }

    }

    private fun registerApi() {


        val map = HashMap<String, String>().apply {
            put("name", act.binding.etName.text.toString())
            put("email", act.binding.etEmail.text.toString())
            put("phone_number", act.binding.etPhnnum.text.toString())
            put("password", act.binding.etPass.text.toString())
            put("device_type", "1")
            put("device_token", "tokeenn")
        }


        getRegisterResp(map)
    }


    fun getRegisterResp(map: HashMap<String, String>){


        if (Utility.isNetworkConnected(act)){
            progressDialog.show(act, "Please wait...")

            var result =  ApiClient.api().registerUser(map,
                //  profileImgUri
            )

            result.enqueue(object :retrofit2.Callback<JsonObject>{
                override fun onResponse(call: Call<JsonObject>, data: Response<JsonObject>) {

                    if (data !=null){
                        progressDialog.dialog.dismiss()

                        var status = data.code()
                        if (status == 200) {

                            val gson = Gson()
                            val signupModel: SignupModel =
                                gson.fromJson(data.body(), SignupModel::class.java)

                            signupResp.postValue(signupModel)
                        }else{
                            Toast.makeText(act,"Something went wrong",Toast.LENGTH_SHORT).show()

                        }

                    }else{
                        progressDialog.dialog.dismiss()
                        Toast.makeText(act,act.getString(R.string.tryagain),Toast.LENGTH_SHORT).show()

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



    fun valid(): Boolean {

        var valid = false

        var name = act.binding.etName.text.trim().toString()
        var email = act.binding.etEmail.text.trim().toString()
        var phonenum = act.binding.etPhnnum.text.trim().toString()
        var pass = act.binding.etPass.text?.trim().toString()


        if (name.isNullOrEmpty()){
            Utility.alertError(act, "Enter name")
            valid = false

        }

        else if (email.isNullOrEmpty()){
            Utility.alertError(act, "Enter email")

        }
        else if (!Utility.isValidEmail(email.toString())) {
            Utility.alertError(act, "Enter valid email")


        }
        else if (phonenum.isNullOrEmpty()) {
            Utility.alertError(act, "Enter phone num")

        }

        else if (phonenum.length <10) {
            Utility.alertError(act, "Enter 10 number ")

        }

        else if (pass.isNullOrEmpty()) {
            Utility.alertError(act, "Enter pass ")

        }
        else if (pass.length < 8) {
            Utility.alertError(act, "Minimum length 8")

        }

        else {
            valid = true
        }


        return valid
    }


}
