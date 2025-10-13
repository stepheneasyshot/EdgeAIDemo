package com.example.edgeaidemo.llamacpp

import android.net.Uri
import android.provider.OpenableColumns
import com.example.edgeaidemo.appContext
import com.stephen.commonhelper.utils.debugLog
import com.stephen.commonhelper.utils.errorLog
import com.stephen.commonhelper.utils.infoLog
import com.stephen.llamacppbridge.GgufFileReader
import com.stephen.llamacppbridge.LlamaCppBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTime

object LLManager {
    private val instance = LlamaCppBridge()
    private var modelInitJob: Job? = null
    private var responseGenerationJob: Job? = null
    private var chat: Chat? = null

    fun load(
        chat: Chat = Chat(),
        absolutePath: String,
        params: LlamaCppBridge.InferenceParams = LlamaCppBridge.InferenceParams(),
        onError: (String) -> Unit,
        onSuccess: () -> Unit,
    ) {
        kotlin.runCatching {
            LLManager.chat = chat
            modelInitJob = CoroutineScope(Dispatchers.Default).launch {
                instance.load(absolutePath, params)
                debugLog("Model loaded")
                if (chat.systemPrompt.isNotEmpty()) {
                    instance.addSystemPrompt(chat.systemPrompt)
                    infoLog("System prompt added")
                }
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }.onFailure {
            onError(it.message.orEmpty())
        }
    }


    fun getResponse(
        query: String,
        responseTransform: (String) -> String,
        onPartialResponseGenerated: (String) -> Unit,
        onSuccess: (String) -> Unit,
        onCancelled: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            assert(chat != null) { "Please call SmolLMManager.create() first." }
            responseGenerationJob =
                CoroutineScope(Dispatchers.Default).launch {
                    var response = ""
                    val duration =
                        measureTime {
                            instance.getResponseAsFlow(query).collect { piece ->
                                response += piece
                                withContext(Dispatchers.Main) {
                                    onPartialResponseGenerated(response)
                                }
                            }
                        }
                    response = responseTransform(response)
                    // once the response is generated
                    // add it to the messages database
                    withContext(Dispatchers.Main) {
                        onSuccess(response)
                    }
                }
        } catch (e: CancellationException) {
            onCancelled()
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun copyModelFile(
        uri: Uri,
        onComplete: (String) -> Unit,
    ) {
        var fileName = ""
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }
        if (fileName.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                appContext.contentResolver.openInputStream(uri).use { inputStream ->
                    FileOutputStream(File(appContext.filesDir, fileName)).use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                }
                val ggufFileReader = GgufFileReader()
                ggufFileReader.load(File(appContext.filesDir, fileName).absolutePath)
                withContext(Dispatchers.Main) {
                    onComplete(fileName)
                }
            }
        } else {
            errorLog("File name is empty")
        }
    }

    suspend fun loadChat(fileName: String, onSuccess: () -> Unit) = withContext(Dispatchers.IO) {
        val path = File(appContext.filesDir, fileName).absolutePath
        LLManager.load(absolutePath = path, onSuccess = {
            infoLog("Model loaded")
            onSuccess()
        }, onError = {
            infoLog("Model load error: $it")
        })
    }

    fun checkGGUFFile(uri: Uri): Boolean {
        appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            val ggufMagicNumberBytes = ByteArray(4)
            inputStream.read(ggufMagicNumberBytes)
            return ggufMagicNumberBytes.contentEquals(byteArrayOf(71, 71, 85, 70))
        }
        return false
    }

    /**
     * 查看内部目录下是否有gguf后缀文件
     */
    fun checkModelFileExist(): List<String> {
        val filesDir = appContext.filesDir
        val files = filesDir.listFiles()
        val modelFiles = mutableListOf<String>()
        files?.forEach { file ->
            if (file.name.endsWith(".gguf")) {
                modelFiles.add(file.name)
            }
        }
        return modelFiles
    }
}