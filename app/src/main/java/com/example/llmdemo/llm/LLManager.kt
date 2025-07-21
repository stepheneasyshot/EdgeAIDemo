package com.example.llmdemo.llm

import com.stephen.commonhelper.utils.debugLog
import com.stephen.commonhelper.utils.infoLog
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.measureTime

object LLManager {
    private val instance = SmolLM()
    private var modelInitJob: Job? = null
    private var responseGenerationJob: Job? = null
    private var chat: Chat? = null

    fun load(
        chat: Chat = Chat(),
        absolutePath: String,
        params: SmolLM.InferenceParams = SmolLM.InferenceParams(),
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
}