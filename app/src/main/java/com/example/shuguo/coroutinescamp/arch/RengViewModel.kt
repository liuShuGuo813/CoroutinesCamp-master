package com.example.shuguo.coroutinescamp.arch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.shuguo.coroutinescamp.Api
import com.example.shuguo.coroutinescamp.model.Repo
import io.reactivex.rxjava3.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 *  @Time : 2021-12-22
 *  @Author : lsg
 *  @Description : JetPack组件中对协程的支持
 */
class RengViewModel : ViewModel(){

    val repo = liveData {
        emit(loadUsers())
    }

    private suspend fun loadUsers(): List<Repo>{
        val retrofit = Retrofit.Builder()
                .baseUrl("https://apu.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
        val api = retrofit.create(Api::class.java)
        return api.listReposKt("rengwuxian")
    }
}