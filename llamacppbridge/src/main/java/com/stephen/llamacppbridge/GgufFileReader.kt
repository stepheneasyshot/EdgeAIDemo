package com.stephen.llamacppbridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GgufFileReader {
    companion object {
        init {
            System.loadLibrary("ggufreader")
        }
    }

    // 存储本地 GGUF 上下文的句柄，初始值为 0L
    private var nativeHandle: Long = 0L

    /**
     * 异步加载 GGUF 模型文件。
     * 该方法会在 IO 线程中调用本地方法获取 GGUF 上下文的本地句柄。
     *
     * @param modelPath GGUF 模型文件的路径。
     */
    suspend fun load(modelPath: String) =
        withContext(Dispatchers.IO) {
            // 调用本地方法获取 GGUF 上下文的本地句柄
            nativeHandle = getGGUFContextNativeHandle(modelPath)
        }

    /**
     * 从已加载的 GGUF 文件中读取上下文大小（以令牌数量为单位）。
     *
     * @return 若成功读取到上下文大小则返回对应的长整型值，若未读取到则返回 null。
     * @throws AssertionError 如果未调用 [load] 方法初始化读取器。
     */
    fun getContextSize(): Long? {
        // 断言本地句柄不为 0，确保读取器已初始化
        assert(nativeHandle != 0L) { "Use GGUFReader.load() to initialize the reader" }
        // 调用本地方法获取上下文大小
        val contextSize = getContextSize(nativeHandle)
        return if (contextSize == -1L) {
            // 若返回 -1L 表示未读取到上下文大小，返回 null
            null
        } else {
            contextSize
        }
    }

    /**
     * 从已加载的 GGUF 文件中读取聊天模板。
     *
     * @return 若成功读取到聊天模板则返回对应的字符串，若未读取到则返回 null。
     * @throws AssertionError 如果未调用 [load] 方法初始化读取器。
     */
    fun getChatTemplate(): String? {
        // 断言本地句柄不为 0，确保读取器已初始化
        assert(nativeHandle != 0L) { "Use GGUFReader.load() to initialize the reader" }
        // 调用本地方法获取聊天模板
        val chatTemplate = getChatTemplate(nativeHandle)
        return chatTemplate.ifEmpty {
            // 若获取到的字符串为空，返回 null
            null
        }
    }

    /**
     * 返回本地句柄（指向在本地创建的 gguf_context 的指针）。
     *
     * @param modelPath GGUF 模型文件的路径。
     * @return 指向 gguf_context 的本地句柄。
     */
    private external fun getGGUFContextNativeHandle(modelPath: String): Long

    /**
     * 根据本地句柄从 GGUF 文件中读取上下文大小（以令牌数量为单位）。
     *
     * @param nativeHandle 指向 gguf_context 的本地句柄。
     * @return 上下文大小，若未读取到则返回 -1L。
     */
    private external fun getContextSize(nativeHandle: Long): Long

    /**
     * 根据本地句柄从 GGUF 文件中读取聊天模板。
     *
     * @param nativeHandle 指向 gguf_context 的本地句柄。
     * @return 聊天模板字符串，若未读取到则返回空字符串。
     */
    private external fun getChatTemplate(nativeHandle: Long): String
}