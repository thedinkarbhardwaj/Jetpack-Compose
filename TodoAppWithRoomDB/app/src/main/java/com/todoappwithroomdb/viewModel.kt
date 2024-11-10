package com.todoappwithroomdb


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.todoappwithroomdb.RoomDb.Todo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Date

class TodoViewModel:ViewModel() {

    var todoDao = MainApplication.todoDatabase.getTodoDao()

    var todoLiveData:LiveData<List<Todo>> = todoDao.getAllTodo()


    @RequiresApi(Build.VERSION_CODES.O)
    fun addTodo(title:String){

        viewModelScope.launch(Dispatchers.IO) {
            todoDao.addTodo(Todo(title = title, createdAt = Date.from(Instant.now())))

        }
    }

    fun deleteTodo(id:Int){
        Log.d("IDDDDDDDDDDDDDDDDDDDDDDD1",id.toString())
        viewModelScope.launch(Dispatchers.IO) {
            todoDao.deleteTodo(id)

        }

    }
}