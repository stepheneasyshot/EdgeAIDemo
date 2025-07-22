#include "LLMInference.h"
#include <jni.h>

/**
 * @brief 加载 LLM 模型。
 *
 * 该函数通过 JNI 从 Java 层调用，用于加载指定路径的 LLM 模型，并返回指向模型实例的指针。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPath 包含模型文件路径的 Java 字符串对象。
 * @param minP 令牌被考虑的最小概率，即核采样（top-P sampling）的阈值。
 * @param temperature 采样温度，控制输出的随机性。
 * @param storeChats 是否存储聊天记录。
 * @param contextSize 模型的上下文大小。
 * @param chatTemplate 包含聊天模板的 Java 字符串对象。
 * @param nThreads 用于推理的线程数。
 * @param useMmap 是否使用内存映射加载模型。
 * @param useMlock 是否锁定模型内存。
 * @return 指向 LLMInference 实例的 jlong 类型指针，若加载失败可能抛出 Java 异常。
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_loadModel(JNIEnv* env, jobject thiz, jstring modelPath, jfloat minP,
                                                         jfloat temperature, jboolean storeChats, jlong contextSize,
                                                         jstring chatTemplate, jint nThreads, jboolean useMmap, jboolean useMlock) {
    // 标识是否复制字符串内容的标志
    jboolean    isCopy           = true;
    // 将 Java 字符串转换为 C 风格的 UTF-8 字符串，获取模型路径
    const char* modelPathCstr    = env->GetStringUTFChars(modelPath, &isCopy);
    // 创建一个新的 LLMInference 实例
    auto*       llmInference     = new LLMInference();
    // 将 Java 字符串转换为 C 风格的 UTF-8 字符串，获取聊天模板
    const char* chatTemplateCstr = env->GetStringUTFChars(chatTemplate, &isCopy);

    try {
        // 调用 LLMInference 实例的 loadModel 方法加载模型
        llmInference->loadModel(modelPathCstr, minP, temperature, storeChats, contextSize, chatTemplateCstr, nThreads,
                                useMmap, useMlock);
    } catch (std::runtime_error& error) {
        // 若加载过程中抛出异常，在 Java 层抛出 IllegalStateException 异常
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
    }

    // 释放之前获取的 C 风格的模型路径字符串
    env->ReleaseStringUTFChars(modelPath, modelPathCstr);
    // 释放之前获取的 C 风格的聊天模板字符串
    env->ReleaseStringUTFChars(chatTemplate, chatTemplateCstr);
    // 将 LLMInference 实例指针转换为 jlong 类型并返回
    return reinterpret_cast<jlong>(llmInference);
}

/**
 * @brief 向聊天记录中添加消息。
 *
 * 该函数通过 JNI 从 Java 层调用，用于向已加载的 LLM 模型的聊天记录中添加消息。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 * @param message 包含要添加消息内容的 Java 字符串对象。
 * @param role 包含消息角色（如 "user", "system" 等）的 Java 字符串对象。
 */
extern "C" JNIEXPORT void JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_addChatMessage(JNIEnv* env, jobject thiz, jlong modelPtr, jstring message,
                                                              jstring role) {
    // 标识是否复制字符串内容的标志
    jboolean    isCopy       = true;
    // 将 Java 字符串转换为 C 风格的 UTF-8 字符串，获取消息内容
    const char* messageCstr  = env->GetStringUTFChars(message, &isCopy);
    // 将 Java 字符串转换为 C 风格的 UTF-8 字符串，获取消息角色
    const char* roleCstr     = env->GetStringUTFChars(role, &isCopy);
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto*       llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    // 调用 LLMInference 实例的 addChatMessage 方法添加消息
    llmInference->addChatMessage(messageCstr, roleCstr);
    // 释放之前获取的 C 风格的消息内容字符串
    env->ReleaseStringUTFChars(message, messageCstr);
    // 释放之前获取的 C 风格的消息角色字符串
    env->ReleaseStringUTFChars(role, roleCstr);
}

/**
 * @brief 获取响应生成速度。
 *
 * 该函数通过 JNI 从 Java 层调用，用于获取已加载的 LLM 模型生成响应的速度。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 * @return 响应生成速度，单位可能因实现而异。
 */
extern "C" JNIEXPORT jfloat JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_getResponseGenerationSpeed(JNIEnv* env, jobject thiz, jlong modelPtr) {
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    // ... 已有代码 ...
    return llmInference->getResponseGenerationTime();
}

/**
 * @brief 获取已使用的上下文大小。
 *
 * 该函数通过 JNI 从 Java 层调用，用于获取已加载的 LLM 模型当前已使用的上下文大小。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 * @return 已使用的上下文大小，单位可能因实现而异。
 */
extern "C" JNIEXPORT jint JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_getContextSizeUsed(JNIEnv* env, jobject thiz, jlong modelPtr) {
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    // 调用 LLMInference 实例的 getContextSizeUsed 方法获取已使用的上下文大小
    return llmInference->getContextSizeUsed();
}

/**
 * @brief 关闭 LLM 模型并释放资源。
 *
 * 该函数通过 JNI 从 Java 层调用，用于关闭已加载的 LLM 模型并释放相关资源。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 */
extern "C" JNIEXPORT void JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_close(JNIEnv* env, jobject thiz, jlong modelPtr) {
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    // 删除 LLMInference 实例，释放资源
    delete llmInference;
}

/**
 * @brief 启动响应生成过程。
 *
 * 该函数通过 JNI 从 Java 层调用，用于启动已加载的 LLM 模型根据给定提示生成响应的过程。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 * @param prompt 包含生成响应所需提示的 Java 字符串对象。
 */
extern "C" JNIEXPORT void JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_startCompletion(JNIEnv* env, jobject thiz, jlong modelPtr, jstring prompt) {
    // 标识是否复制字符串内容的标志
    jboolean    isCopy       = true;
    // 将 Java 字符串转换为 C 风格的 UTF-8 字符串，获取提示内容
    const char* promptCstr   = env->GetStringUTFChars(prompt, &isCopy);
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto*       llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    // 调用 LLMInference 实例的 startCompletion 方法启动响应生成过程
    llmInference->startCompletion(promptCstr);
    // 释放之前获取的 C 风格的提示内容字符串
    env->ReleaseStringUTFChars(prompt, promptCstr);
}

/**
 * @brief 循环生成响应片段。
 *
 * 该函数通过 JNI 从 Java 层调用，用于在已启动响应生成过程后，循环获取 LLM 模型生成的响应片段。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 * @return 包含生成响应片段的 Java 字符串对象，若出现异常则返回 nullptr。
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_completionLoop(JNIEnv* env, jobject thiz, jlong modelPtr) {
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    try {
        // 调用 LLMInference 实例的 completionLoop 方法获取响应片段
        std::string response = llmInference->completionLoop();
        // 将 C++ 字符串转换为 Java 字符串并返回
        return env->NewStringUTF(response.c_str());
    } catch (std::runtime_error& error) {
        // 若生成过程中抛出异常，在 Java 层抛出 IllegalStateException 异常
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}

/**
 * @brief 停止响应生成过程。
 *
 * 该函数通过 JNI 从 Java 层调用，用于停止已启动的 LLM 模型响应生成过程。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPtr 指向 LLMInference 实例的 jlong 类型指针。
 */
extern "C" JNIEXPORT void JNICALL
Java_com_stephen_llamacppbridge_LlamaCppBridge_stopCompletion(JNIEnv* env, jobject thiz, jlong modelPtr) {
    // 将 jlong 类型的指针转换为 LLMInference 实例指针
    auto* llmInference = reinterpret_cast<LLMInference*>(modelPtr);
    // 调用 LLMInference 实例的 stopCompletion 方法停止响应生成过程
    llmInference->stopCompletion();
}