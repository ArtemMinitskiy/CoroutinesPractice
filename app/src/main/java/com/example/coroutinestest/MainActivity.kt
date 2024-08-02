package com.example.coroutinestest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.coroutinestest.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    //Dispatchers.Default: применяется по умолчанию, если тип диспетчера не указан явным образом. Этот тип использует общий пул разделяемых фоновых потоков и подходит для вычислений, которые не работают с операциями ввода-вывода (операциями с файлами, базами данных, сетью) и которые требуют интенсивного потребления ресурсов центрального процессора.
    //
    //Dispatchers.IO: использует общий пул потоков, создаваемых по мере необходимости, и предназначен для выполнения операций ввода-вывода (например, операции с файлами или сетевыми запросами).
    //
    //Dispatchers.Main: применяется в графических приложениях, например, в приложениях Android или JavaFX.
    //
    //Dispatchers.Unconfined: корутина не закреплена четко за определенным потоком или пулом потоков. Она запускается в текущем потоке до первой приостановки. После возобновления работы корутина продолжает работу в одном из потоков, который сторого не фиксирован. Разработчики языка Kotlin в обычной ситуации не рекомендуют использовать данный тип.
    //
    //newSingleThreadContext и newFixedThreadPoolContext: позволяют вручную задать поток/пул для выполнения корутины
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    //The CoroutineExceptionHandler is an optional element of a CoroutineContext allowing you to handle uncaught exceptions
    //Exceptions will be caught if these requirements are met:
    //When ⏰: The exception is thrown by a coroutine that automatically throws exceptions (works with launch, not with async).
    //Where 🌍: If it’s in the CoroutineContext of a CoroutineScope or a root coroutine (direct child of CoroutineScope or a supervisorScope).
    private val handler = CoroutineExceptionHandler { _, exception ->
        println("Caught $exception")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.apply {
            btnSimpleCoroutine.setOnClickListener {
                //launch(), как правило, применяется, когда нам не надо возвращать результат из корутины и когда нам ее надо выполнять одновременно с другим кодом
                scope.launch {
                    main()
                }
            }
            btnSimpleCoroutineScope.setOnClickListener {
                scope.launch {
                    main2()
                }
            }
            btnRunBlocking.setOnClickListener {
                scope.launch {
                    main3()
                }
            }
            btnWithoutJoin.setOnClickListener {
                scope.launch {
                    mainWithoutJoin()
                }
            }
            btnWithJoin.setOnClickListener {
                scope.launch {
                    mainWithJoin()
                }
            }
            btnWithoutStart.setOnClickListener {
                scope.launch {
                    mainWithoutStart()
                }
            }
            btnWithStart.setOnClickListener {
                scope.launch {
                    mainWithStart()
                }
            }
            btnAsync.setOnClickListener {
                scope.launch {
                    mainWithAsync()
                }
            }
            btnWithAwait.setOnClickListener {
                scope.launch {
                    mainWithAwait()
                }
            }
            btnParallelLaunch.setOnClickListener {
                scope.launch {
                    mainParallelLaunch()
                }
            }
            btnDelayedStartWithAwait.setOnClickListener {
                scope.launch {
                    mainDelayedStartWithAwait()
                }
            }
            btnWithCancel.setOnClickListener {
                scope.launch {
                    mainWithCancel()
//                    mainCancelAndJoin()
                }
            }
            btnCancellationException.setOnClickListener {
                scope.launch {
                    mainCancellationException()
                }
            }
            btnCancellationExceptionAsync.setOnClickListener {
                scope.launch {
                    mainCancellationExceptionAsync()
                }
            }
            btnCoroutineExceptionHandler.setOnClickListener {

                //will be caught by the handler
                scope.launch(handler) {
                    launch {
                        throw Exception("Failed coroutine")
                    }
                }
                //won’t be caught
                //The exception isn’t caught because the handler is not installed in the right CoroutineContext.
                //The inner launch will propagate the exception up to the parent as soon as it happens, since the parent doesn’t know anything about the handler, the exception will be thrown.
//                scope.launch {
//                    launch(handler) {
//                        throw Exception("Failed coroutine")
//                    }
//                }
            }
            btnWithTimeOut.setOnClickListener {
                scope.launch(Dispatchers.Default) {
                    println("Starting the long calculation...")

                    //using withTimeOut function
                    //which runs the coroutine for 3sec
                    //the coroutine will automatically be canceled and don’t do any further calculations when a certain time has elapsed and withTimeOut()
                    withTimeout(3000L) {
                        for (i in 30..50) {
                            if (isActive) println("Result for i = $i : ${fib(i)}")
                        }
                    }
                    println("Ending the long calculation...")
                }
            }

            btnYield.setOnClickListener {
                scope.launch(Dispatchers.Default) {
                    yieldFun()
                }
            }

            btnSupervisorJob.setOnClickListener {
                scope.launch(Dispatchers.Default) {
                    supervisorJob()
                }
            }
        }
    }

    //SupervisorJob — это специальный тип работы (Job) в Kotlin Coroutines, который предоставляет механизм для изоляции ошибок в дочерних корутинах от родительской корутины.
    //Он был создан для обеспечения независимости выполнения корутин, чтобы ошибка в одной дочерней корутине не приводила к автоматической отмене других корутин.
    //В этом примере мы добавляем SupervisorJob в CoroutineScope. Теперь каждая дочерняя корутина, запущенная в этом скоупе, будет иметь независимость в отношении отмены.
    //В случае возникновения ошибки в дочерней корутине остальные корутины будут продолжать выполнение.
    private fun supervisorJob() {
        val exceptionHandler= CoroutineExceptionHandler { _, throwable ->
            println("Handle exception: ${throwable.message}")
        }
        val supervisorJob = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + supervisorJob + exceptionHandler)

        scope.launch {
            launch {
                delay(3_000)
                throw (IllegalStateException("Child coroutine failed"))
            }
            while (true) {
                delay(1_000)
                println("tick 1")
            }
        }

        scope.launch {
            while (true) {
                delay(1_000)
                println("tick 2")
            }
        }
    }

    //yield — это функция приостановки, которая сигнализирует планировщику корутины о готовности корутины приостановить свое выполнение, тем самым позволяя другим корутинам использовать поток.
    //Это особенно полезно в сценариях, где корутина участвует в длительной операции, вызов yield гарантирует, что поток не будет монополизирован, что позволяет более справедливо выполнять параллельные задачи.
    //yield в первой корутине приводит к тому, что первая корутина фактически уступает место второй.
    private fun yieldFun() = runBlocking {
        launch {
            for (i in 1..5) {
                println("Computation $i in Coroutine 1")
                yield()
            }
        }
        launch {
            for (i in 1..5) {
                println("Task $i in Coroutine 2")
            }
        }
    }

    private fun fib(n: Int): Long {
        return when (n) {
            0 -> 0
            1 -> 1
            else -> fib(n - 1) + fib(n - 2)
        }
    }

    //A job is returned
/*
    val job = GlobalScope.launch(Dispatchers.Default) {
        println("Some Coroutines")
        delay(400L)
    }
*/

    private suspend fun main() {
        for (i in 0..5) {
            delay(400L)
            println(i)
        }

        println("Hello Coroutines")
    }

    //coroutineScope не блокирует вызывающий поток, а просто приостанавливает выполнение, освобождая поток для использования другими ресурсами
    private suspend fun main2() = coroutineScope {
        launch {
            for (i in 0..5) {
                delay(400L)
                println(i)
            }
            println("Goodbye Coroutines")
        }

        println("Hello Coroutines")
    }

    //runBlocking блокирует вызывающий поток, пока все корутины внутри вызова runBlocking { ... } не завершат свое выполнение
    private fun main3() = runBlocking {
        launch {
            for (i in 0..5) {
                delay(400L)
                println(i)
            }
            println("Goodbye Coroutines")
        }

        println("Hello Coroutines")
    }

    private suspend fun main4() = coroutineScope {
        launch {
            println("Outer coroutine")
            launch {
                println("Inner coroutine")
                delay(400L)
            }
        }

        println("End of Main")
    }

    private suspend fun mainWithoutJoin() = coroutineScope {
        launch {
            for (i in 1..5) {
                println(i)
                delay(400L)
            }
        }

        println("Start")
        println("End")
    }

    //join() позволяет ожидать, пока корутина не завершится
    private suspend fun mainWithJoin() = coroutineScope {
        val job = launch {
            for (i in 1..5) {
                println(i)
                delay(400L)
            }
        }

        println("Start")
        //ожидаем завершения корутины
        job.join()
        println("End")
    }

    private suspend fun mainWithoutStart() = coroutineScope {
        //корутина создана и запущена
        launch() {
            delay(200L)
            println("Coroutine has started")
        }

        delay(1000L)
        println("Other actions in main method")
    }

    //По умолчанию построитель корутин launch создает и сразу же запускает корутину. Однако Kotlin также позволяет применять технику отложенного запуска корутины (lazy-запуск), при котором корутина запускается при вызове метода start() объекта Job.
    //Для установки отложенного запуска в функцию launch() передается значение start = CoroutineStart.LAZY
    private suspend fun mainWithStart() = coroutineScope {
        //корутина создана, но не запущена
        val job = launch(start = CoroutineStart.LAZY) {
            delay(200L)
            println("Coroutine has started")
        }

        delay(1000L)
        //запускаем корутину
        job.start()
        println("Other actions in main method")
    }

    //async запускает отдельную корутину, которая выполняется параллельно с остальными корутинами
    //async возвращает объект Deferred, который ожидает получения результата корутины.
    //Интерфейс Deferred унаследован от интерфейса Job, поэтому для него также доступен весь функционал, определенный для интефейса Job
    private suspend fun mainWithAsync() = coroutineScope {
        async { printHello() }
        println("Program has finished")
    }

    private suspend fun printHello() {
        delay(500L)
        println("Hello work!")
    }

    //Для получения результата из объекта Deferred применяется функция await()
    private suspend fun mainWithAwait() = coroutineScope {
        val message: Deferred<String> = async { getMessage() }
        println("message: ${message.await()}")
        println("Program has finished")
    }

    private suspend fun getMessage(): String {
        delay(500L)
        return "Hello"
    }

    private suspend fun mainParallelLaunch() = coroutineScope {
        val numDeferred1 = async { sum(1, 2) }
        val numDeferred2 = async { sum(3, 4) }
        val numDeferred3 = async { sum(5, 6) }
        val num1 = numDeferred1.await()
        val num2 = numDeferred2.await()
        val num3 = numDeferred3.await()

        println("number1: $num1  number2: $num2  number3: $num3")
    }

    private suspend fun sum(a: Int, b: Int): Int {
        delay(500L)
        return a + b
    }

    private suspend fun mainDelayedStartWithAwait() = coroutineScope {
        val sum = async(start = CoroutineStart.LAZY) { sumDelayedStartWithAwait(1, 2) }
        delay(1000L)
        println("Actions after the coroutine creation")
        println("sum: ${sum.await()}")
    }

    private fun sumDelayedStartWithAwait(a: Int, b: Int): Int {
        println("Coroutine has started")
        return a + b
    }

    private suspend fun mainWithCancel() = coroutineScope {
        val downloader: Job = launch {
            println("Начинаем загрузку файлов")
            for (i in 1..5) {
                println("Загружен файл $i")
                delay(500L)
            }
        }
        delay(800L)
        println("Надоело ждать, пока все файлы загрузятся. Прерву-ка я загрузку...")
        //отменяем корутину
        downloader.cancel()
        //ожидаем завершения корутины
        downloader.join()
        println("Работа программы завершена")
    }

    private suspend fun mainCancelAndJoin() = coroutineScope {
        val downloader: Job = launch {
            println("Начинаем загрузку файлов")
            for (i in 1..5) {
                println("Загружен файл $i")
                delay(500L)
            }
        }
        delay(800L)
        println("Надоело ждать, пока все файлы загрузятся. Прерву-ка я загрузку...")
        //отменяем корутину и ожидаем ее завершения
        downloader.cancelAndJoin()
        println("Работа программы завершена")
    }

    private suspend fun mainCancellationException() = coroutineScope {
        val downloader: Job = launch {
            try {
                println("Начинаем загрузку файлов")
                for (i in 1..5) {
                    println("Загружен файл $i")
                    delay(500L)
                }
            } catch (e: CancellationException) {
                println("Загрузка файлов прервана")
            } finally {
                println("Загрузка завершена")
            }
        }
        delay(800L)
        println("Надоело ждать. Прерву-ка я загрузку...")
        downloader.cancelAndJoin()
        println("Работа программы завершена")
    }

    private suspend fun mainCancellationExceptionAsync() = coroutineScope {
        //создаем и запускаем корутину
        val message = async {
            getMessage()
        }
        //отмена корутины
        message.cancelAndJoin()

        try {
            //ожидаем получение результата
            println("message: ${message.await()}")
        } catch (e: CancellationException) {
            println("Coroutine has been canceled")
        }
        println("Program has finished")
    }

}