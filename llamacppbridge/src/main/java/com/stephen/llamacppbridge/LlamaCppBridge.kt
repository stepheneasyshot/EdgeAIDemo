package com.stephen.llamacppbridge

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

/**
 * LlamaCppBridge 类用于与 Llama C++ 库进行交互，实现大语言模型（LLM）的加载、推理等功能。
 * 该类会根据设备的 CPU 特性加载不同的本地库，支持加载 GGUF 格式的模型文件，
 * 并提供了添加对话消息、获取模型响应等功能。
 */
class LlamaCppBridge {
    companion object {
        init {
            val logTag = LlamaCppBridge::class.java.simpleName

            // 检查以下 CPU 特性是否可用，并据此加载本地库
            val cpuFeatures = getCPUFeatures()
            val hasFp16 = cpuFeatures.contains("fp16") || cpuFeatures.contains("fphp")
            val hasDotProd = cpuFeatures.contains("dotprod") || cpuFeatures.contains("asimddp")
            val hasSve = cpuFeatures.contains("sve")
            val hasI8mm = cpuFeatures.contains("i8mm")
            val isAtLeastArmV82 =
                cpuFeatures.contains("asimd") &&
                        cpuFeatures.contains("crc32") &&
                        cpuFeatures.contains(
                            "aes",
                        )
            val isAtLeastArmV84 = cpuFeatures.contains("dcpop") && cpuFeatures.contains("uscat")

            Log.d(logTag, "CPU features: $cpuFeatures")
            Log.d(logTag, "- hasFp16: $hasFp16")
            Log.d(logTag, "- hasDotProd: $hasDotProd")
            Log.d(logTag, "- hasSve: $hasSve")
            Log.d(logTag, "- hasI8mm: $hasI8mm")
            Log.d(logTag, "- isAtLeastArmV82: $isAtLeastArmV82")
            Log.d(logTag, "- isAtLeastArmV84: $isAtLeastArmV84")

            // 检查应用是否在模拟器设备上运行
            // 注意，这不是官方检查模拟器的方法
            val isEmulated =
                (Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu"))
            Log.d(logTag, "isEmulated: $isEmulated")

            if (!isEmulated) {
                if (supportsArm64V8a()) {
                    if (isAtLeastArmV84 && hasSve && hasI8mm && hasFp16 && hasDotProd) {
                        Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod_i8mm_sve.so")
                        System.loadLibrary("smollm_v8_4_fp16_dotprod_i8mm_sve")
                    } else if (isAtLeastArmV84 && hasSve && hasFp16 && hasDotProd) {
                        Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod_sve.so")
                        System.loadLibrary("smollm_v8_4_fp16_dotprod_sve")
                    } else if (isAtLeastArmV84 && hasI8mm && hasFp16 && hasDotProd) {
                        Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod_i8mm.so")
                        System.loadLibrary("smollm_v8_4_fp16_dotprod_i8mm")
                    } else if (isAtLeastArmV84 && hasFp16 && hasDotProd) {
                        Log.d(logTag, "Loading libsmollm_v8_4_fp16_dotprod.so")
                        System.loadLibrary("smollm_v8_4_fp16_dotprod")
                    } else if (isAtLeastArmV82 && hasFp16 && hasDotProd) {
                        Log.d(logTag, "Loading libsmollm_v8_2_fp16_dotprod.so")
                        System.loadLibrary("smollm_v8_2_fp16_dotprod")
                    } else if (isAtLeastArmV82 && hasFp16) {
                        Log.d(logTag, "Loading libsmollm_v8_2_fp16.so")
                        System.loadLibrary("smollm_v8_2_fp16")
                    } else {
                        Log.d(logTag, "Loading libsmollm_v8.so")
                        System.loadLibrary("smollm_v8")
                    }
                } else if (Build.SUPPORTED_32_BIT_ABIS[0]?.equals("armeabi-v7a") == true) {
                    // armv7a (32位) 设备
                    Log.d(logTag, "Loading libsmollm_v7a.so")
                    System.loadLibrary("smollm_v7a")
                } else {
                    Log.d(logTag, "Loading default libsmollm.so")
                    System.loadLibrary("smollm")
                }
            } else {
                // 加载不包含 ARM 特定指令的默认本地库
                Log.d(logTag, "Loading default libsmollm.so")
                System.loadLibrary("smollm")
            }
        }

        /**
         * 读取 /proc/cpuinfo 文件，返回以 'Features :' 开头的包含可用 CPU 特性的行。
         * @return 包含 CPU 特性的字符串，如果文件不存在则返回空字符串。
         */
        private fun getCPUFeatures(): String {
            val cpuInfo =
                try {
                    File("/proc/cpuinfo").readText()
                } catch (e: FileNotFoundException) {
                    ""
                }
            val cpuFeatures =
                cpuInfo
                    .substringAfter("Features")
                    .substringAfter(":")
                    .substringBefore("\n")
                    .trim()
            return cpuFeatures
        }

        /**
         * 检查设备是否支持 arm64-v8a 架构。
         * @return 如果支持则返回 true，否则返回 false。
         */
        private fun supportsArm64V8a(): Boolean = Build.SUPPORTED_ABIS[0].equals("arm64-v8a")
    }

    private var nativePtr = 0L

    /**
     * 提供推理参数的默认值。
     * 当用户未提供相应参数或 GGUF 模型文件中没有这些参数时，将使用这些默认值。
     */
    object DefaultInferenceParams {
        val contextSize: Long = 1024L
        val chatTemplate: String =
            "{% for message in messages %}{% if loop.first and messages[0]['role'] != 'system' %}{{ '<|im_start|>system You are a helpful AI assistant named SmolLM, trained by Hugging Face<|im_end|> ' }}{% endif %}{{'<|im_start|>' + message['role'] + ' ' + message['content'] + '<|im_end|>' + ' '}}{% endfor %}{% if add_generation_prompt %}{{ '<|im_start|>assistant ' }}{% endif %}"
    }

    /**
     * 数据类，用于保存 LLM 的推理参数。
     *
     * @param minP 令牌被考虑的最小概率，也称为核采样（top-P sampling）。（默认值：0.01f）
     * @param temperature 采样温度，值越高输出越随机。（默认值：1.0f）
     * @param storeChats 是否在内存中存储聊天历史。如果为 true，LLM 将记住当前会话中的先前交互。（默认值：true）
     * @param contextSize LLM 的上下文大小（以令牌为单位），决定了 LLM 能“记住”多少之前的对话。
     *                    如果为 null，将使用 GGUF 模型文件中的值，若模型文件中也没有，则使用默认值。（默认值：null）
     * @param chatTemplate 用于格式化对话的聊天模板，是一个 Jinja2 模板字符串。
     *                     如果为 null，将使用 GGUF 模型文件中的值，若模型文件中也没有，则使用默认值。（默认值：null）
     * @param numThreads 用于推理的线程数。（默认值：4）
     * @param useMmap 是否使用内存映射文件 I/O 来加载模型，这可以提高加载速度并减少内存使用。（默认值：true）
     * @param useMlock 是否将模型锁定在内存中，这可以防止模型被交换到磁盘，可能提高性能。（默认值：false）
     */
    data class InferenceParams(
        val minP: Float = 0.01f,
        val temperature: Float = 1.0f,
        val storeChats: Boolean = true,
        val contextSize: Long? = null,
        val chatTemplate: String? = null,
        val numThreads: Int = 4,
        val useMmap: Boolean = true,
        val useMlock: Boolean = false,
    )

    /**
     * 从给定路径加载 GGUF 模型。
     * 该函数将读取 GGUF 模型文件中的元数据，如上下文大小和聊天模板，如果 `params` 中未明确提供这些参数，将使用模型文件中的值。
     *
     * @param modelPath GGUF 模型文件的路径。
     * @param params 要使用的推理参数。如果未提供，将使用默认值。
     *               如果 `params` 中未提供 `contextSize` 或 `chatTemplate`，将使用 GGUF 模型文件中的值，
     *               如果模型文件中也没有，则使用 [DefaultInferenceParams] 中的默认值。
     * @return 如果模型加载成功返回 `true`，否则返回 `false`。
     * @throws FileNotFoundException 如果在给定路径下找不到模型文件。
     */
    suspend fun load(
        modelPath: String,
        params: InferenceParams = InferenceParams(),
    ) = withContext(Dispatchers.IO) {
        val ggufFileReader = GgufFileReader()
        ggufFileReader.load(modelPath)
        val modelContextSize = ggufFileReader.getContextSize() ?: DefaultInferenceParams.contextSize
        val modelChatTemplate =
            ggufFileReader.getChatTemplate() ?: DefaultInferenceParams.chatTemplate
        nativePtr =
            loadModel(
                modelPath,
                params.minP,
                params.temperature,
                params.storeChats,
                params.contextSize ?: modelContextSize,
                params.chatTemplate ?: modelChatTemplate,
                params.numThreads,
                params.useMmap,
                params.useMlock,
            )
    }

    /**
     * 向聊天历史中添加用户消息。
     * 该消息将在生成下一个响应时作为对话的一部分被考虑。
     *
     * @param message 用户的消息。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun addUserMessage(message: String) {
        verifyHandle()
        addChatMessage(nativePtr, message, "user")
    }

    /**
     * 为 LLM 添加系统提示。
     *
     * @param prompt 系统提示内容。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun addSystemPrompt(prompt: String) {
        verifyHandle()
        addChatMessage(nativePtr, prompt, "system")
    }

    /**
     * 为 LLM 推理添加助手消息。
     * 助手消息是 LLM 对之前对话查询的响应。
     *
     * @param message 助手消息内容。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun addAssistantMessage(message: String) {
        verifyHandle()
        addChatMessage(nativePtr, message, "assistant")
    }

    /**
     * 返回 LLM 通过 `getResponse()` 生成上一个响应的速度（令牌/秒）。
     *
     * @return 响应生成速度。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun getResponseGenerationSpeed(): Float {
        verifyHandle()
        return getResponseGenerationSpeed(nativePtr)
    }

    /**
     * 返回 LLM 上下文窗口消耗的令牌数量。
     * LLM 的上下文大致是 tokenize(apply_chat_template(messages_in_conversation)) 的输出。
     *
     * @return 上下文使用的令牌数量。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun getContextLengthUsed(): Int {
        verifyHandle()
        return getContextSizeUsed(nativePtr)
    }

    /**
     * 以异步 Flow 的形式返回 LLM 对给定查询的响应。
     * 这对于流式传输 LLM 生成的响应很有用。
     *
     * @param query 向 LLM 提出的查询。
     * @return 一个字符串 Flow，每个字符串是响应的一部分。
     *         当 LLM 完成响应生成时，Flow 结束。特殊令牌 "[EOG]"（生成结束）表示响应结束。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun getResponseAsFlow(query: String): Flow<String> =
        flow {
            verifyHandle()
            startCompletion(nativePtr, query)
            var piece = completionLoop(nativePtr)
            while (piece != "[EOG]") {
                emit(piece)
                piece = completionLoop(nativePtr)
            }
            stopCompletion(nativePtr)
        }

    /**
     * 以字符串形式返回 LLM 对给定查询的响应。
     * 该函数是阻塞的，将返回完整的响应。
     *
     * @param query 用户向 LLM 提出的查询/提示。
     * @return LLM 的完整响应。
     * @throws IllegalStateException 如果模型未加载。
     */
    fun getResponse(query: String): String {
        verifyHandle()
        startCompletion(nativePtr, query)
        var piece = completionLoop(nativePtr)
        var response = ""
        while (piece != "[EOG]") {
            response += piece
            piece = completionLoop(nativePtr)
        }
        stopCompletion(nativePtr)
        return response
    }

    /**
     * 卸载 LLM 模型并释放资源。
     * 当不再需要 SmolLM 实例时，应调用此方法以防止内存泄漏。
     */
    fun close() {
        if (nativePtr != 0L) {
            close(nativePtr)
            nativePtr = 0L
        }
    }

    /**
     * 验证模型是否已加载。
     * @throws IllegalStateException 如果模型未加载。
     */
    private fun verifyHandle() {
        assert(nativePtr != 0L) { "Model is not loaded. Use SmolLM.create to load the model" }
    }

    /**
     * 加载模型的本地方法。
     * @param modelPath 模型文件路径。
     * @param minP 最小概率。
     * @param temperature 采样温度。
     * @param storeChats 是否存储聊天记录。
     * @param contextSize 上下文大小。
     * @param chatTemplate 聊天模板。
     * @param nThreads 线程数。
     * @param useMmap 是否使用内存映射。
     * @param useMlock 是否锁定内存。
     * @return 模型指针。
     */
    private external fun loadModel(
        modelPath: String,
        minP: Float,
        temperature: Float,
        storeChats: Boolean,
        contextSize: Long,
        chatTemplate: String,
        nThreads: Int,
        useMmap: Boolean,
        useMlock: Boolean,
    ): Long

    /**
     * 添加聊天消息的本地方法。
     * @param modelPtr 模型指针。
     * @param message 消息内容。
     * @param role 消息角色。
     */
    private external fun addChatMessage(
        modelPtr: Long,
        message: String,
        role: String,
    )

    /**
     * 获取响应生成速度的本地方法。
     * @param modelPtr 模型指针。
     * @return 响应生成速度。
     */
    private external fun getResponseGenerationSpeed(modelPtr: Long): Float

    /**
     * 获取上下文使用大小的本地方法。
     * @param modelPtr 模型指针。
     * @return 上下文使用大小。
     */
    private external fun getContextSizeUsed(modelPtr: Long): Int

    /**
     * 关闭模型的本地方法。
     * @param modelPtr 模型指针。
     */
    private external fun close(modelPtr: Long)

    /**
     * 开始完成任务的本地方法。
     * @param modelPtr 模型指针。
     * @param prompt 提示内容。
     */
    private external fun startCompletion(
        modelPtr: Long,
        prompt: String,
    )

    /**
     * 完成循环的本地方法。
     * @param modelPtr 模型指针。
     * @return 生成的片段。
     */
    private external fun completionLoop(modelPtr: Long): String

    /**
     * 停止完成任务的本地方法。
     * @param modelPtr 模型指针。
     */
    private external fun stopCompletion(modelPtr: Long)
}