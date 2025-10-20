package com.example.edgeaidemo.litert

import com.example.edgeaidemo.appContext
import com.stephen.commonhelper.utils.errorLog
import com.stephen.commonhelper.utils.infoLog
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

object LiteRTLoadManager {
    private lateinit var interpreter: Interpreter
    private lateinit var gpuDelegate: GpuDelegate

    fun init() {
        // 1. 配置 LiteRT 解释器选项
        val options = Interpreter.Options()

        // 2. [可选] 如果添加了 GPU 依赖，可以设置 GPU Delegate
        // **注意：** 独立的 GPU Delegate 在某些设备上可能不如 Play Services 托管的稳定。
        gpuDelegate = GpuDelegate()
        options.addDelegate(gpuDelegate)

        // 3. 创建解释器实例
        try {
            interpreter = Interpreter(loadModelFile(), options)
            infoLog("LiteRTLoadManager init success")
        } catch (e: Exception) {
            // 处理加载模型时的错误
            errorLog("LiteRTLoadManager init fail")
            e.printStackTrace()
            return
        }
    }

    fun run(data: Any) {
        // --- 假设输入和输出张量大小 ---
        // 示例：输入形状 [1, 224, 224, 3] (Float32)
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()

        // 创建输入和输出缓冲区
        val inputBuffer = ByteBuffer.allocateDirect(
            inputShape[0] * inputShape[1] * inputShape[2] * inputShape[3] * 4 // 4 bytes for float
        ).apply { order(ByteOrder.nativeOrder()) }

        val outputBuffer = ByteBuffer.allocateDirect(
            outputShape[0] * outputShape[1] * 4 // 4 bytes for float
        ).apply { order(ByteOrder.nativeOrder()) }

        // [TODO] 准备您的输入数据，并将其写入 inputBuffer
        // 例如：将您的图像数据转换为 float 数组并写入 inputBuffer

        // 4. 运行模型推理
        interpreter.run(inputBuffer, outputBuffer)

        // 5. [TODO] 处理 outputBuffer 中的结果
    }

    fun close() {
        interpreter.close()
        gpuDelegate.close()
    }

    /**
     * 从 assets 文件夹加载 .tflite 模型文件
     */
    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = appContext.assets.openFd("gemma3-1b-it-int4.litertlm")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}