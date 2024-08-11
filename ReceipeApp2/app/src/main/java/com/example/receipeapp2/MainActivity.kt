package com.example.receipeapp2

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.receipeapp2.Utils.Utility
import com.example.receipeapp2.databinding.ActivityMainBinding
import itemadpt

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var viewmodel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewmodel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewmodel.init(this)



        viewmodel.itemResp.observe(this, { categoryResponse ->
            viewmodel.adpt = itemadpt(this, viewmodel.itemlist)
            binding.recItem.layoutManager = LinearLayoutManager(this)
            binding.recItem.adapter = viewmodel.adpt
        })

        setupSearchView()

        getITEMObserver()
    }

    private fun getITEMObserver() {
        viewmodel.itemResp.observe(this, Observer<CategoryResponse>() {

            if (it != null) {




            } else {
                Utility.alertError(this@MainActivity,"something wrong")
            }


        })

    }

    private fun setupSearchView() {
        binding.etSearch.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewmodel.adpt.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewmodel.adpt.filter.filter(newText)
                return true
            }
        })
    }
}