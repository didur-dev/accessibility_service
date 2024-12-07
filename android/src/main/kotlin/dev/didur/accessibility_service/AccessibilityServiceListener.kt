package dev.didur.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.flutter.embedding.android.FlutterView
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class AccessibilityServiceListener : AccessibilityService() {
    private val executor: ExecutorService = Executors.newFixedThreadPool(4)


    private val differenceThreshold = 5.0       // Limite de 5% de diferença
    private val maxFiles = 10                   // Limite de 10 imagens no cache

    private var lastScreenshotTime: Long = 0
    private var lastBitmap: Bitmap? = null      // Armazena o último bitmap capturado
    private var now: Date? = null

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

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

        private const val CACHE_SIZE: Int = 100
        var nodeMap: LruCache<String, AccessibilityNodeInfo> = LruCache(CACHE_SIZE)

        fun getNodeInfo(id: String?): AccessibilityNodeInfo {
            return nodeMap[id]
        }
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

        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        executor.shutdown()
        Log.d(Constants.LOG_TAG, "onDestroy")
    }

    override fun onInterrupt() {
        Log.d(Constants.LOG_TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            Settings.loadPreferences(applicationContext)

            val className = it.className.nullableString()
            val packageName = it.packageName.nullableString()
            val eventType = it.eventType
            val text = it.text.joinToString(separator = " ~~ ")
            val description = it.contentDescription.nullableString()
            val source = event.source

            now = Date()

            val eventWarper = EventWrapper(packageName, className, mapEventType(eventType), dateTime = now)

            executor.execute {
                if(Settings.recusaAutomatica  || Settings.leituraAcessibilidadeAtivado){
                    val result = analyze(source, eventWarper)

                    if(Settings.leituraAcessibilidadeAtivado) {
                        val intent: Intent = Intent(Constants.ACCESSIBILITY_INTENT)
                        intent.putExtra(Constants.SEND_BROADCAST, Gson().toJson(result.toMap()))
                        sendBroadcast(intent)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if(Settings.leituraOcrAtivado) {
                        takeScreenshotForOCR(eventWarper)
                    }
                }
            }
        }
    }

    private fun generateNodeId(node: AccessibilityNodeInfo): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault())
        val datePart = dateFormat.format(now!!)
        return  datePart +  "_" + node.windowId.toString() + "_" + node.className + "_" + node.text + "_" + node.contentDescription
    }

    private fun getNodesByDate(date: Date): MutableList<ClickableElement> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault())
        val dateString = dateFormat.format(date)

        val resultList = mutableListOf<ClickableElement>()
        for (i in 0 until nodeMap.size()) {
            val key = nodeMap.snapshot().keys.elementAt(i)
            if (key.startsWith(dateString)) {
                val node = nodeMap.get(key)
                if (node != null) {
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    resultList.add(
                        ClickableElement(
                            id = key,
                            text = node.text?.toString(),
                            bounds = bounds
                        )
                    )
                }
            }
        }
        return resultList
    }


    private fun extractClickableElements(): MutableList<ClickableElement> {
        val rootNode = rootInActiveWindow ?: return mutableListOf()
        val result = AnalyzedResult()
        analyzeTree(rootNode, result)
        return result.clickableElements ?: mutableListOf()
    }

    private fun mapEventType(eventType: Int?) : String{
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
        val result = AnalyzedResult(event = event, clickableElements = mutableListOf())
        analyzeTree(source, result)
        return result
    }

    private fun analyzeTree(node: AccessibilityNodeInfo?, result: AnalyzedResult, depth: List<Int>? = null) {
        if (node == null) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val trace = depth ?: listOf(0)
        val data = NodeData(
            depth = trace,
            info = node,
            bounds = bounds,
        )

        result.nodes[trace.joinToString("-")] = data

        // Salva o nó clicável no cache
        if (node.isClickable) {
            val nodeId = generateNodeId(node);

            val clickableElement = ClickableElement(
                id = nodeId,
                text = node.text?.toString(),
                bounds = bounds
            )
            result.clickableElements.add(clickableElement);

            nodeMap.put(nodeId, node);
        }

        if (node.childCount > 0) {
            for (i in 0 until node.childCount) {
                analyzeTree(node.getChild(i), result, trace + i)
            }
        }
    }

    // Método que será chamado para capturar a tela
    @RequiresApi(Build.VERSION_CODES.R)
    fun takeScreenshotForOCR(eventWrapper: EventWrapper) {
        val executor: Executor = Executors.newSingleThreadExecutor()

        // Callback que recebe o bitmap da screenshot
        val screenshotCallback = object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                // Obter o bitmap da screenshot
                val bitmap: Bitmap? = screenshot.hardwareBuffer.let {
                    Bitmap.wrapHardwareBuffer(it, screenshot.colorSpace)
                }

                // Processar o bitmap e retornar o texto extraído
                bitmap?.let {
                    processScreenshot(it, eventWrapper)
                } ?: run {
                    Log.e(Constants.LOG_TAG, "Falha ao capturar o bitmap")
                }
            }

            override fun onFailure(errorCode: Int) {
                Log.e(Constants.LOG_TAG,  "Falha ao tirar screenshot. Código de erro: $errorCode")
            }
        }

        val currentTime = System.currentTimeMillis()

        // Verifica se já passou o tempo mínimo de 2 segundos
        if (currentTime - lastScreenshotTime >= Settings.ocrIntervalo) {
            lastScreenshotTime = currentTime

            takeScreenshot(Display.DEFAULT_DISPLAY, executor, screenshotCallback)
        }
    }

    // Processa a captura de tela e compara com o bitmap anterior
    @RequiresApi(Build.VERSION_CODES.O)
    private fun processScreenshot(newBitmap: Bitmap, eventWrapper: EventWrapper) {
        lastBitmap?.let {
            // Comparação com o bitmap anterior
            val differencePercentage = getBitmapDifferencePercentageOptimized(it, newBitmap, step = 10)

            if (differencePercentage > differenceThreshold) {
                Log.d(Constants.LOG_TAG, "Diferença de ${String.format("%.2f", differencePercentage)}% detectada, executando OCR...")
                // Executar OCR somente se a diferença for significativa
                executeOCR(newBitmap, eventWrapper)
            } else {
                Log.d(Constants.LOG_TAG, "Diferença de ${String.format("%.2f", differencePercentage)}%, ignorando OCR.")
            }
        } ?: run {
            // Se não houver bitmap anterior, execute OCR
            Log.d(Constants.LOG_TAG, "Primeira captura, executando OCR...")
            executeOCR(newBitmap, eventWrapper)
        }

        // Atualiza o bitmap anterior para o novo
        lastBitmap = newBitmap
    }

    private fun convertToBitmapWithAccess(bitmap: Bitmap): Bitmap {
        // Converte o Bitmap para ARGB_8888, permitindo acesso aos pixels
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getBitmapDifferencePercentageOptimized(bitmap1: Bitmap, bitmap2: Bitmap, step: Int = 10): Double {
        // Verifica se os bitmaps estão em Config.HARDWARE e converte se necessário
        val bitmap1Converted = if (bitmap1.config == Bitmap.Config.HARDWARE) convertToBitmapWithAccess(bitmap1) else bitmap1
        val bitmap2Converted = if (bitmap2.config == Bitmap.Config.HARDWARE) convertToBitmapWithAccess(bitmap2) else bitmap2

        if (bitmap1Converted.width != bitmap2Converted.width || bitmap1Converted.height != bitmap2Converted.height) {
            return 100.0 // Se as dimensões forem diferentes, considere 100% de diferença
        }

        var differentPixels = 0
        var checkedPixels = 0

        // Verifica pixel a pixel, mas saltando um número de pixels definido pelo 'step'
        for (x in 0 until bitmap1Converted.width step step) {
            for (y in 0 until bitmap1Converted.height step step) {
                if (bitmap1Converted.getPixel(x, y) != bitmap2Converted.getPixel(x, y)) {
                    differentPixels++
                }
                checkedPixels++ // Conta os pixels verificados
            }
        }

        // Calcula a porcentagem de pixels diferentes em relação aos pixels verificados
        return (differentPixels.toDouble() / checkedPixels) * 100
    }

    private fun binarizeBitmap(original: Bitmap): Bitmap {
        // Converte o bitmap para um formato compatível, caso seja um hardware bitmap
        val compatibleBitmap = original.copy(Bitmap.Config.ARGB_8888, true)

        // Cria um bitmap em escala de cinza
        val grayscale = Bitmap.createBitmap(compatibleBitmap.width, compatibleBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = filter
        canvas.drawBitmap(compatibleBitmap, 0f, 0f, paint)

        // Binariza o bitmap
        val binarized = Bitmap.createBitmap(grayscale.width, grayscale.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until grayscale.width) {
            for (y in 0 until grayscale.height) {
                val pixel = grayscale.getPixel(x, y)
                val intensity = Color.red(pixel)
                if (intensity < 128) {
                    binarized.setPixel(x, y, Color.BLACK)
                } else {
                    binarized.setPixel(x, y, Color.WHITE)
                }
            }
        }

        // Libera o bitmap original e o grayscale para economizar memória
        compatibleBitmap.recycle()
        grayscale.recycle()

        return binarized
    }


    // Função para executar OCR (substitua com a sua lógica de OCR)
    private fun executeOCR(bitmap: Bitmap, eventWrapper: EventWrapper) {
        Log.d(Constants.LOG_TAG, "Executando OCR na imagem...")

        lateinit var image: InputImage

        if(Settings.ocrBinarize) {
            // Binariza o bitmap antes do OCR
            val binarizedBitmap = binarizeBitmap(bitmap)
            image = InputImage.fromBitmap(binarizedBitmap, 0)
        } else if (Settings.ocrUpscale) {
            val scale = 1.5f // Fator de upscale (ajuste se necessário)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true)
            image = InputImage.fromBitmap(scaledBitmap, 0)
        } else {
            image = InputImage.fromBitmap(bitmap, 0)
        }

        // Obtenha uma instância do TextRecognizer
        val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Processa a imagem
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = processTextRecognitionResult(visionText)

                var clickableElements = mutableListOf<ClickableElement>();
                if(Settings.recusaAutomatica) {
                    clickableElements = getNodesByDate(now!!)
                }

                var imagePath : String? = null
                Log.d(Constants.LOG_TAG, "Salvar print: ${Settings.tirarPrintSolicitacaoCorrida}")
                if(Settings.tirarPrintSolicitacaoCorrida){
                    val fileName = "${eventWrapper.packageName}_${System.currentTimeMillis()}"
                    imagePath = saveBitmapToCache(bitmap, fileName)
                }

                // Envia o texto e a imagem para o flutter
                val intent = Intent(Constants.ACCESSIBILITY_INTENT)
                intent.putExtra(Constants.SEND_BROADCAST, Gson().toJson(AnalyzedResult(text = text, imagePath = imagePath, clickableElements = clickableElements, event = eventWrapper)))
                sendBroadcast(intent)

            }
            .addOnFailureListener { e ->
                Log.e(Constants.LOG_TAG, "Erro ao reconhecer texto: ${e.message}")
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


    // Função para salvar o Bitmap no cache, limitando a 10 arquivos
    private fun saveBitmapToCache(bitmap: Bitmap, fileName: String): String {
        val cacheDir = cacheDir
        val file = File(cacheDir, "image_$fileName.png")

        // Salva o Bitmap como PNG no cache
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, outputStream)
        outputStream.flush()
        outputStream.close()

        // Limita o cache a 10 arquivos
        limitCacheFiles(cacheDir, maxFiles)

        return file.absolutePath
    }

    // Função para limitar o número de arquivos no cache
    private fun limitCacheFiles(cacheDir: File, maxFiles: Int) {
        val files = cacheDir.listFiles()
        if (files != null && files.size > maxFiles) {
            // Ordena os arquivos por data de modificação (os mais antigos primeiro)
            files.sortedBy { it.lastModified() }.take(files.size - maxFiles).forEach { it.delete() }
        }
    }
}

