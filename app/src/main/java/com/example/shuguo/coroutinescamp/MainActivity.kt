package com.example.shuguo.coroutinescamp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.example.shuguo.coroutinescamp.databinding.ActivityMainBinding
import com.example.shuguo.coroutinescamp.model.Repo
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleObserver
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @Time : 2021-12-22
 * @Author : lsg
 * @Description : 首页
 *  tips：
 *  1.协程的性能会稍稍弱于Rxjava，但区别不大，目前协程推出Flow等API，未来可能会补全链式调用这块的缺口
 *  目前JetPack的很多组件也开始支持协程了，所以使用协程是个不错的选择。
 *  2.JetPack中提供了很多对于协程的支持，如lifecycleScope、viewModelScope，使用者使用的时候不需要管理生命
 *  周期，可以更便捷，安全的使用。
 *  3.协程是一个线程库，它是基于线程的，作为一个线程的上层库，它优于线程，并且更简洁使用。
 *  4.协程、RxJava如何切到主线程的，底层都是Handler.post()方法切换的。
 **/
class MainActivity : AppCompatActivity() {
    //协程的对象引用
    var job: Job? = null
    //Rx的对象引用
    var disposable: Disposable? = null
    //Kotlin提供的主线程环境的协程，可以创建 也可关闭，多个创建也可以
    var scope = MainScope()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /**
         * Kotlin中的协程不同于广义的协程，Kotlin最后会运行在JVM环境中，代码由kt -> dex的转变
         * 本质是一个解决并发任务的方案，同时也是这个方案的一个组件
         * 优点：1.耗时任务自动切换后台，避免UI卡顿
         *      2.自动切换线程
         */
        GlobalScope.launch(Dispatchers.Main) {
            ioCode1()
            uiCode1()
        }

        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))//使用Rxjava形式将请求默认放在IO线程执行
                .build()
        val api = retrofit.create(Api::class.java)
        //Retrofit原本请求方式 回调
//        requestByRetrofit(api, binding)
        //Retrofit + 协程请求方式 自动切换线程
//        requestByCoroutines(api, binding)
        //Retrofit + Rxjava请求方式
//        requestByRx(api, binding)
        //模拟正常嵌套请求 需合并两个请求的返回值
//        mergeTwoRequestByRxNested(api, binding)
        //使用Rxjava zip操作符来合并两个请求
//        mergeTwoRequestByRxZip(api, binding)
        //使用协程来合并两个请求
        mergeTwoRequestByCoroutines(api, binding)
        scope.launch {
            withContext(Dispatchers.IO){
                print("展示Kotlin自带的主线程协程，使用它创建协程时不需要指定主线程参数")
            }
        }
        lifecycleScope.launch{
            withContext(Dispatchers.IO){
                print("展示KTX带来的协程，在Activity中可以方便使用，依赖于生命周期 无需手动取消")
            }
        }
        lifecycleScope.launchWhenCreated{
            withContext(Dispatchers.IO){
                print("展示KTX带来的协程，在Activity中可以在某个生命周期后自动开启此协程")
            }
        }

    }

    /**
     * async和launch是同等级的 都会创建一个协程出来
     * 区别就是async是一个可以有结果的协程，他的结果不是通过返回值的形式获取的
     * 而是通过调用的形式，即调用await()方法
     * await是一个挂起函数
     */
    private fun mergeTwoRequestByCoroutines(api: Api, binding: ActivityMainBinding) {
        job = GlobalScope.launch(Dispatchers.Main) {
            val one = async { api.listReposKt("rengwuxian") }
            val two = async { api.listReposKt("liuShuGuo813") }
            binding.textView.text = "${one.await()[0].name} - ${two.await()[0].name}"
        }
    }

    private fun mergeTwoRequestByRxZip(api: Api, binding: ActivityMainBinding) {
        Single.zip(
                api.listReposRx("rengwuxian"),
                api.listReposRx("liuShuGuo813"),
                { list1, list2 -> "${list1[0].name} - ${list2[0].name}" }
        ).observeOn(AndroidSchedulers.mainThread())
                .subscribe { combined -> binding.textView.text = combined }
    }

    private fun mergeTwoRequestByRxNested(api: Api, binding: ActivityMainBinding) {
        api.listReposRx("rengwuxian")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<Repo>> {
                    override fun onSubscribe(d: Disposable?) {
                        disposable = d
                    }

                    override fun onSuccess(repos: List<Repo>?) {
                        api.listReposRx("liuShuGuo813")
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object : SingleObserver<List<Repo>> {
                                    override fun onSubscribe(d: Disposable?) {

                                    }

                                    override fun onSuccess(reposs: List<Repo>?) {
                                        binding.textView.text = "${repos?.get(0)?.name} - ${reposs?.get(0)?.name}"
                                    }

                                    override fun onError(e: Throwable?) {
                                        binding.textView.text = e?.message
                                    }

                                })
                    }

                    override fun onError(e: Throwable?) {
                        binding.textView.text = e?.message
                    }

                })
    }

    private fun requestByRx(api: Api, binding: ActivityMainBinding) {
        api.listReposRx("rengwuxian")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<List<Repo>> {
                    override fun onSubscribe(d: Disposable?) {

                    }

                    override fun onSuccess(repos: List<Repo>?) {
                        binding.textView.text = repos?.get(0)?.name
                    }

                    override fun onError(e: Throwable?) {
                        binding.textView.text = e?.message
                    }

                })
    }

    private fun requestByCoroutines(api: Api, binding: ActivityMainBinding) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = api.listReposKt("rengwuxian")
                binding.textView.text = response[0].name
            } catch (e: Exception) {
                binding.textView.text = e.message
            }
        }
    }

    private fun requestByRetrofit(api: Api, binding: ActivityMainBinding) {
        api.listRepos("rengwuxian")
                .enqueue(object : Callback<List<Repo>> {
                    override fun onResponse(call: Call<List<Repo>>, response: Response<List<Repo>>) {
                        binding.textView.text = response.body()?.get(0)?.name
                    }

                    override fun onFailure(call: Call<List<Repo>>, t: Throwable) {

                    }

                })
    }

    private suspend fun ioCode1() {
        withContext(Dispatchers.IO) {
            print("ioCode1 ${Thread.currentThread().name}")
        }
    }

    private fun uiCode1() {
        print("uiCode1 ${Thread.currentThread().name}")
    }

    override fun onDestroy() {
        /**
         * 关闭页面时通过协程的Cancel方法关闭协程任务，避免因活跃线程持有Activity引用无法被GC回收而导致协程泄露
         * 协程泄露的本质也就是线程泄露
         */
        job?.cancel()
        //取消RxJava
        disposable?.dispose()
        scope.cancel()
        super.onDestroy()
    }
}