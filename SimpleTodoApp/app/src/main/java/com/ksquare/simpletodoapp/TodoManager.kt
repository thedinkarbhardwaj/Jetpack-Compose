package com.ksquare.simpletodoapp

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.Instant
import java.util.Date

object TodoManager {

    var todoList = mutableListOf<Todo>()

    fun getTodo():List<Todo>{
        return todoList
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addTodo(title:String){
        todoList.add(Todo(System.currentTimeMillis().toInt(),title, Date.from(Instant.now())))

    }

    fun deleteTodo(id:Int){

        Log.d("IDDDDDDDDDDDDDDDDDDDDDDD",id.toString())
        todoList.removeIf {
            it.id == id
        }
    }
}