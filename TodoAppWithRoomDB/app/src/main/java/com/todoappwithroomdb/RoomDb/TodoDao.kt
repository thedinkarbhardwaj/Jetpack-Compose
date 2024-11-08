package com.todoappwithroomdb.RoomDb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TodoDao {

    @Query("Select * from TODO")
    fun getAllTodo():LiveData<List<Todo>>

    @Insert
    fun addTodo(todo: Todo)

    @Query("DELETE from TODO WHERE id = :id")
    fun deleteTodo(id:Int)
}