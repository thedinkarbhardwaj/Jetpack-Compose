package com.ksquare.simpletodoapp.viewModel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ksquare.simpletodoapp.Todo
import com.ksquare.simpletodoapp.TodoManager

class TodoViewModel:ViewModel() {
    var _todoList = MutableLiveData<List<Todo>>()
    var todoLiveData:LiveData<List<Todo>> = _todoList

    fun getAllTodo(){
        _todoList.value = TodoManager.getTodo().reversed()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addTodo(title:String){

        TodoManager.addTodo(title)
        getAllTodo()
    }

    fun deleteTodo(id:Int){
        Log.d("IDDDDDDDDDDDDDDDDDDDDDDD1",id.toString())

        TodoManager.deleteTodo(id)
        getAllTodo()

    }
}