package com.example.receipeapp2.Login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.receipeapp2.MainActivity
import com.example.receipeapp2.R
import com.example.receipeapp2.Signup.SignupModel
import com.example.receipeapp2.Utils.Utility
import com.example.receipeapp2.databinding.ActivityLoginBinding

class LoginAct : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityLoginBinding
    lateinit var viewmodel: LoginViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewmodel = ViewModelProvider(this).get(LoginViewModel::class.java)
        viewmodel.init(this)

        setUI()
        getSigninObserver()

    }

    private fun setUI() {

        binding.btnLogin.setOnClickListener(this)
        binding.txtRegister.setOnClickListener(this)


    }

    private fun getSigninObserver() {
        viewmodel.signinResp.observe(this, Observer<SignupModel>() {

            if (it != null) {
                var status = it.statusCode // 200 for registeration success

                if (status == 200) {

                    Utility.successAlert(this, it.message)



                    Handler(Looper.myLooper()!!).postDelayed({

                        var intentv = Intent(this@LoginAct,MainActivity::class.java)
                        startActivity(intentv)
                        finish()

                    }, 2000)


                } else {
                    Utility.alertError(this@LoginAct, it.message.toString())
                }
            }

        })

    }


    override fun onClick(view: View?) {

        when(view){
            binding.btnLogin->{

                viewmodel.getLoginApi()

            }

        }

    }

}
