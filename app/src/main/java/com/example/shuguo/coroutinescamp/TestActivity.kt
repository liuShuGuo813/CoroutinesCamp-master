package com.example.shuguo.coroutinescamp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.shuguo.coroutinescamp.databinding.ActivityTestBinding
import kotlinx.coroutines.*

/**
 * @Time : 2021-12-23
 * @Author : lsg
 * @Description : 练习示例二
 **/
class TestActivity : AppCompatActivity(),CoroutineScope by MainScope() {
  private val TAG = "TestActivity"
  private lateinit var binding: ActivityTestBinding

  companion object{
    fun start(context: Context){
      val intent = Intent(context,TestActivity::class.java)
      context.startActivity(intent)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityTestBinding.inflate(layoutInflater)
    setContentView(binding.root)
    title = "练习示例二"
    launch {
      var data = getData()
      var processData = processData(data)
      Log.e(TAG,"setText ${Thread.currentThread().name}")
      // 练习内容：用协程让上面 ↑ 这两行放在后台执行，然后把代码截图贴到腾讯课堂的作业里
      binding.textView.text = processData
    }

  }

  // 耗时函数 1
  private suspend fun getData() = withContext(Dispatchers.IO){
    Log.e(TAG,"getData ${Thread.currentThread().name}")
    // 假设这个函数比较耗时，需要放在后台
    return@withContext "hen_coder"
  }

  // 耗时函数 2
  private suspend fun processData(data: String) = withContext(Dispatchers.IO){
    Log.e(TAG,"processData ${Thread.currentThread().name}")
    // 假设这个函数也比较耗时，需要放在后台
    return@withContext data.split("_") // 把 "hen_coder" 拆成 ["hen", "coder"]
            .map { it.capitalize() } // 把 ["hen", "coder"] 改成 ["Hen", "Coder"]
            .reduce { acc, s -> acc + s }// 把 ["Hen", "Coder"] 改成 "HenCoder"
  }

  override fun onDestroy() {
    cancel()
    super.onDestroy()
  }
}