package com.example.receipeapp2.Signup

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.receipeapp2.Login.LoginAct
import com.example.receipeapp2.MainActivity
import com.example.receipeapp2.R
import com.example.receipeapp2.Utils.Utility
import com.example.receipeapp2.databinding.ActivitySignupBinding

class Signup : AppCompatActivity(), View.OnClickListener {

    lateinit var binding: ActivitySignupBinding
    lateinit var viewmodel: SignupViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewmodel = ViewModelProvider(this).get(SignupViewModel::class.java)
        viewmodel.init(this)

        setUi()
        getSignupObserver()
    }

    private fun setUi() {


        binding.btnSignup.setOnClickListener(this)
        binding.txtLogin.setOnClickListener(this)

    }

    private fun getSignupObserver() {
        viewmodel.signupResp.observe(this, Observer<SignupModel>() {

            if (it != null) {
                var status = it.statusCode // 200 for registeration success

                if (status == 200) {


                    Utility.successAlert(this, it.message)

                    Handler(Looper.myLooper()!!).postDelayed({

                        var intentv = Intent(this@Signup, MainActivity::class.java)
                        startActivity(intentv)
                        finish()

                    }, 2000)

                } else {
                    Utility.alertError(this@Signup, it.message.toString())
                }
            }

        })

    }


    override fun onClick(view: View?) {
        when (view) {
            binding.btnSignup -> {

                viewmodel.register()
            }

            binding.txtLogin -> {
                var intentv = Intent(this@Signup, LoginAct::class.java)
                startActivity(intentv)
                finish()
            }
        }

    }
}