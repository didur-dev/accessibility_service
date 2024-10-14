package dev.didur.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.flutter.embedding.android.FlutterView
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper
import com.google.mlkit.vision.text.Text


class AccessibilityServiceListener : AccessibilityService() {
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)

    // var callback: ((AccessibilityEvent?, AnalyzedResult) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())

    private var lastText: String = ""

    companion object {
        // Exported Accessibility Service Instance
        var instance: AccessibilityServiceListener? = null

        // Is Accessibility Service Granted
        val isGranted: Boolean get() = instance != null

        private var analyzeTreeCallback: ((AccessibilityEvent, AnalyzedResult) -> Unit)? = null

        fun setAnalyzeTreeCallback(callback: ((AccessibilityEvent, AnalyzedResult) -> Unit)?) {
            analyzeTreeCallback = callback
        }

        private var mWindowManager: WindowManager? = null
        private var mOverlayView: FlutterView? = null
    }

    // Get Root Node, if get error, return null
    override fun getRootInActiveWindow() = try {
        super.getRootInActiveWindow()
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        handler.post(runnable)

        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacks(runnable)

        instance = null
        executor.shutdown()
        Log.d(Constants.LOG_TAG, "onDestroy")
    }

    override fun onInterrupt() {
        Log.d(Constants.LOG_TAG, "onInterrupt")
    }

    private val runnable = object : Runnable {
        override fun run() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                extractTextFromImage() { extractedText ->
                    if(lastText != extractedText){
                        val intent: Intent = Intent(Constants.ACCESSIBILITY_INTENT)
                        intent.putExtra(Constants.SEND_BROADCAST, Gson().toJson(AnalyzedResult(text = extractedText)))
                        sendBroadcast(intent)
                        lastText = extractedText
                    }
                }
            }

            // Agenda a próxima execução após 2 segundos (2000 milissegundos)
            handler.postDelayed(this, 2000)
        }
    }



    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val className = it.className.nullableString()
            val packageName = it.packageName.nullableString()
            val eventType = it.eventType
            val text = it.text.joinToString(separator = " ~~ ")
            val description = it.contentDescription.nullableString()
            val source = event.source

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                extractTextFromImage() { extractedText ->
//                    val eventMap = mapOf(
//                        "event" to AnalyzedResult(text = extractedText, event = eventWrapper),
//                        "type" to "text"
//                    )
//                    val intent: Intent = Intent(Constants.ACCESSIBILITY_INTENT)
//                    intent.putExtra(Constants.SEND_BROADCAST, Gson().toJson(eventMap))
//                    sendBroadcast(intent)
//                }
//            }

            executor.execute {
                // Thread.sleep(100)
//                    val start = System.currentTimeMillis()
                var eventWarper: EventWrapper? = null;

                if (className.isNotBlank() && packageName.isNotBlank()) {
                    eventWarper = EventWrapper(packageName, className, mapEventType(eventType))
                }

                val result = analyze(source, eventWarper)

                val intent: Intent = Intent(Constants.ACCESSIBILITY_INTENT)
                intent.putExtra(Constants.SEND_BROADCAST, Gson().toJson(result.toMap()))
                sendBroadcast(intent)

//                    Log.d(Constants.LOG_TAG, "analyze tree cost ${System.currentTimeMillis() - start}ms")
            }


        }
    }

    fun mapEventType(eventType: Int?) : String{
        var value = "";
        when (eventType){
            AccessibilityEvent.TYPE_VIEW_CLICKED -> value = "TYPE_VIEW_CLICKED"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> value = "TYPE_VIEW_LONG_CLICKED"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> value = "TYPE_VIEW_SELECTED"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> value = "TYPE_VIEW_FOCUSED"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> value = "TYPE_VIEW_TEXT_CHANGED"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> value = "TYPE_WINDOW_STATE_CHANGED"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> value = "TYPE_NOTIFICATION_STATE_CHANGED"
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> value = "TYPE_VIEW_HOVER_ENTER"
            AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> value = "TYPE_VIEW_HOVER_EXIT"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> value = "TYPE_TOUCH_EXPLORATION_GESTURE_START"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> value = "TYPE_TOUCH_EXPLORATION_GESTURE_END"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> value = "TYPE_WINDOW_CONTENT_CHANGED"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> value = "TYPE_VIEW_SCROLLED"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> value = "TYPE_VIEW_TEXT_SELECTION_CHANGED"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> value = "TYPE_ANNOUNCEMENT"
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY -> value = "TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> value = "TYPE_GESTURE_DETECTION_START"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> value = "TYPE_GESTURE_DETECTION_END"
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> value = "TYPE_TOUCH_INTERACTION_START"
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> value = "TYPE_TOUCH_INTERACTION_END"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> value = "TYPE_WINDOWS_CHANGED"
            AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> value = "TYPE_VIEW_CONTEXT_CLICKED"
        }

        return value;
    }


    fun analyze(source: AccessibilityNodeInfo?, event: EventWrapper? = null): AnalyzedResult {
        val result = AnalyzedResult(event = event)
        analyzeTree(source, result)
        return result
    }

    private fun analyzeTree(node: AccessibilityNodeInfo?, list: AnalyzedResult, depth: List<Int>? = null) {
        if (node == null) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val trace = depth ?: listOf(0)
        val data = NodeData(
            depth = trace,
            info = node,
            bounds = bounds,
        )

        list.nodes[trace.joinToString("-")] = data

        if (node.childCount > 0) {
            for (i in 0 until node.childCount) {
                analyzeTree(node.getChild(i), list, trace + i)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun extractTextFromImage(onTextExtracted: (String) -> Unit) {
        val executor: Executor = Executors.newSingleThreadExecutor()

        // Callback que recebe o bitmap da screenshot
        val screenshotCallback = object : AccessibilityService.TakeScreenshotCallback {
            override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                // Obter o bitmap da screenshot
                val bitmap: Bitmap? = screenshot.hardwareBuffer?.let {
                    Bitmap.wrapHardwareBuffer(it, screenshot.colorSpace)
                }

                // Processar o bitmap e retornar o texto extraído
                bitmap?.let {
                    processBitmap(it) { extractedText ->
                        onTextExtracted(extractedText)
                    }
                } ?: run {
                    Log.d("BITMAP_ERROR", "Falha ao capturar o bitmap")
                    onTextExtracted("Erro ao capturar o bitmap")
                }
            }

            override fun onFailure(errorCode: Int) {
                Log.d("SCREENSHOT_FAILED", "Falha ao tirar screenshot. Código de erro: $errorCode")
                onTextExtracted("Erro ao tirar screenshot. Código de erro: $errorCode")
            }
        }

        // Tirar a screenshot
        takeScreenshot(Display.DEFAULT_DISPLAY, executor, screenshotCallback)
    }


    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            // Diretório temporário do cache
            val cacheDir = cacheDir  // Ou use externalCacheDir para o cache externo

            // Crie o arquivo no cache
            val fileName = "screenshot_${System.currentTimeMillis()}.jpg"
            val file = File(cacheDir, fileName)

            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()

            Log.i("AccessibilityPlugin", "Screenshot salva no cache: ${file.absolutePath}")
            file.absolutePath  // Retorna o caminho completo do arquivo
        } catch (e: Exception) {
            Log.i("AccessibilityPlugin", "Erro ao salvar a screenshot: ${e.message}")
            null
        }
    }

    private fun processBitmap(bitmap: Bitmap, onComplete: (String) -> Unit) {
        // Crie um InputImage a partir do bitmap
        val image = InputImage.fromBitmap(bitmap, 0)

        // Obtenha uma instância do TextRecognizer
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Processa a imagem
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Retorna o texto completo extraído

                onComplete(processTextRecognitionResult(visionText))
//                onComplete(visionText.text)


            }
            .addOnFailureListener { e ->
                // Em caso de falha, retorna uma mensagem de erro
                onComplete("Erro ao reconhecer texto: ${e.message}")
            }
    }

    private fun processTextRecognitionResult(result: Text): String {
        // Lista de blocos de texto
        val textBlocks = result.textBlocks

        // Ordena os blocos de texto com base na posição vertical (topo da boundingBox)
        val sortedBlocks = textBlocks.sortedWith(compareBy { it.boundingBox?.top ?: 0 })

        val extractedText = StringBuilder()

        // Itera sobre os blocos ordenados e adiciona o texto ao StringBuilder
        for (block in sortedBlocks) {
            for (line in block.lines) {
                extractedText.append(line.text)
                extractedText.append("\n")  // Adiciona uma nova linha após cada linha de texto
            }
        }

        return extractedText.toString()
    }
}

