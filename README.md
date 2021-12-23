# 协程

### Kotlin 协程是什么？

- #### 协程是什么？

  - 协程是一种在程序中处理并发任务的方案，也是这个方案的一个组件。

  - 它和线程属于一个层级的概念，是一种和线程不同的并发任务解决方案：一

    套系统（可以是操作系统，也可以是⼀种编程语⾔）可以选择不同的⽅案来

    处理并发任务，你可以使⽤线程，也可以使⽤协程。

- #### Kotlin的协程是什么？

  - Kotlin 的协程和⼴义的协程不是⼀种东⻄，Kotlin 的协程（确切说是 Kotlin

    for Java 的协程）是⼀个线程框架。

### Kotlin的代码怎么写？

#### ⽤协程来开启后台任务（其实就是后台线程）:

```kotlin
GlobalScope.launch(Dispatchers.IO) {
            print("${Thread.currentThread().name}")
        }
```

#### ⽤协程来开启反复切换线程的任务：

```kotlin
GlobalScope.launch(Dispatchers.Main) {
            ioCode1()
            uiCode1()
}
private suspend fun ioCode1() {
    withContext(Dispatchers.IO) {
        print("ioCode1 ${Thread.currentThread().name}")
    }
}

private fun uiCode1() {
    print("uiCode1 ${Thread.currentThread().name}")
}
```

###  写法总结：

- ⽤ launch() 来开启⼀段协程 ⼀般需要指定 Dispatchers.Main

- 把要在后台⼯作的函数，写成 suspend 函数 ⽽且需要在内部调⽤其他 suspend

  函数来真正切线程

- 按照⼀条线写下来，线程会⾃动切换

###  如果用线程可以实现吗？

嵌套很麻烦

```kotlin
thread {
 	ioCode1()
 	runOnUiThread {
 		uiCode1()
 		thread {
 			ioCode2()
 			runOnUiThread {
 				uiCode2()
 				thread {
 					ioCode3()
 					runOnUiThread {
 						uiCode3()
 					}
 				}
 			}
 		}
 	}
}
```

### 协程的额外天然优势：性能

- 程序什么时候会需要切线程？

  - ⼯作⽐较耗时：放在后台
  - ⼯作⽐较特殊：放在指定线程——⼀般来说，是主线程

- 「耗时代码」⽆法完美判断，导致程序的部分性能问题。

- 协程让函数的创建者可以对耗时代码进⾏标记（使⽤ suspend 关键字），从⽽

  所有耗时代码会 100% 放在后台执⾏，这⽅⾯的性能问题被彻底解决。

- 不⽤协程为什么不⾏？

  - 因为协程切换的关键点是「⾃动」「切回来」，⽽这两点需要给挂起函数提

    供充⾜的上下⽂，即「我应该往哪回」

  - 这是直接使⽤线程⽆法做到的，或者说，⾮常难做到（有多难？参考协程源

    代码）。

###  再回顾：Kotlin的协程是什么？

- 线程框架
- 可以⽤看起来同步的代码写出实质上异步的操作
  - 关键亮点⼀：耗时函数⾃动后台，从⽽提⾼性能
  - 关键亮点⼆：线程的「⾃动切回」

###  **suspend** 关键字

- 并不是用来切线程的
- 关键作用：标记和提醒
- 可以辅助编译，但不是Kotlin这个语言层面的特性

### 使用协程需要注意的

- 在页面销毁或者不用协程的时候调用`cancel()`方法取消掉
- 避免因活跃线程持有Activity引用无法被GC回收而导致内存泄露

### Retrofit对协程的支持

```kotlin
@GET("users/{user}/repos")
  suspend fun listReposKt(@Path("user") user: String): List<Repo>
```

```kotlin
GlobalScope.launch(Dispatchers.Main) {
            try {
                val response = api.listReposKt("rengwuxian")
                binding.textView.text = response[0].name
            } catch (e: Exception) {
                binding.textView.text = e.message
            }
}
```

Retrofit与RxJava请求

```kotlin
@GET("users/{user}/repos")
  fun listReposRx(@Path("user") user: String): Single<List<Repo>>
```

```kotlin
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
```

```kotlin
.addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))//使用Rxjava形式将请求默认放在IO线程执行
```

### 协程和RxJava

- 都可以切线程

- 都不需要嵌套

- 都很强大，应用场景越来越接近

- RxJava 需要回调和包装，协程只需要在保证在协程里调用就行

- 协程的性能会稍稍弱于Rxjava，但区别不大，目前协程推出Flow等API，未来可能会补全链式调用这块的缺口

  目前JetPack的很多组件也开始支持协程了，所以使用协程是个不错的选择。

### **协程和 Architecture Components** 

- 协程泄露，本质上是线程泄露，内存泄露，本质上是对象泄露
- CoroutineScope：「结构化并发」，结构化管理协程
- 目前Jetpack的Lifecycle、ViewModel、LiveData、Room 组件也对协程提供了支持，如下方示例

#### Lifecycle对协程的支持

```kotlin
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
```

#### ViewModel对协程的支持

```kotlin
class RengViewModel : ViewModel(){

    val repo = liveData {
        emit(loadUsers())
    }

    private suspend fun loadUsers(): List<Repo>{
        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
        val api = retrofit.create(Api::class.java)
        return api.listReposKt("rengwuxian")
    }
}
```

### 协程和线程

- 协程和线程分别是什么？

  - 线程就是线程，协程是一个线程

- 库协程和线程哪个容易使？

  - 协程，作为线程的上层库，肯定比线程好用
  - 协程对比线程，不如对比Executor，都是基于线程封装
    - 一般还是协程，优于其他框架主要一个原因就是消除回调

- 协程相比线程的优势和缺陷？

  - 优势：好用，强大
  - 缺陷：上手难度高

- 同Handler相比？

  - 首先，其实没法比，它俩也不是一个维度的东西。Handler 相当于一个「只

    负责 Android 中切线程」的特殊场景化的 Executor，在 Android 中你要

    想让协程切到主线程，还是得用 Handler。

  - 如果我就是要强行对比协程和 Handler，它有什么优劣？

    - 我们要真是从易用性上面来说，你用协程来往主线程切，还真的是比直接用 Handler 更好写、更方便的。

### 本质探秘

- 协程是怎么切线程的？
  - 最终还是使用了原生的线程切换（以及 Android 的 Handler）

- 协程为什么可以从主线程「挂起」，却不卡主线程？
  - 因为所谓的「从主线程挂起」，其实是结束了在主线程的执行，码放在了后台线程执行，以及在后台线程的工作做完后，再把更通过 又抛回主线程

- 协程的 delay() 和 Thread.sleep()delay() 性能更好吗？
  - 并没有那它为什么不卡线程？它只是不卡当前线程而去卡了别的线程

### 对比RxJava与协程代码

##### 场景：模拟正常嵌套请求 需合并两个请求的返回值

- RxJava

```kotlin
private fun mergeTwoRequestByRxZip(api: Api, binding: ActivityMainBinding) {
        Single.zip(
                api.listReposRx("rengwuxian"),
                api.listReposRx("liuShuGuo813"),
                { list1, list2 -> "${list1[0].name} - ${list2[0].name}" }
        ).observeOn(AndroidSchedulers.mainThread())
                .subscribe { combined -> binding.textView.text = combined }
    }
```

- 协程

```kotlin
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
```

[练习项目地址]: https://github.com/liuShuGuo813/CoroutinesCamp-master

