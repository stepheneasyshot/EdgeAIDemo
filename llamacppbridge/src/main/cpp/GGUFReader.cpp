#include "gguf.h"
#include <jni.h>
#include <string>

/**
 * @brief 获取 GGUF 上下文的本地句柄。
 *
 * 该函数通过 Java Native Interface (JNI) 从 Java 层调用，用于根据给定的模型文件路径
 * 初始化 GGUF 上下文，并返回其本地句柄。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param modelPath 包含 GGUF 模型文件路径的 Java 字符串对象。
 * @return 指向初始化后的 GGUF 上下文的 jlong 类型句柄，若初始化失败可能为 nullptr 对应的 jlong 值。
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_stephen_llamacppbridge_GgufFileReader_getGGUFContextNativeHandle(JNIEnv *env, jobject thiz,
                                                                          jstring modelPath) {
    jboolean isCopy = true;
    const char *modelPathCStr = env->GetStringUTFChars(modelPath, &isCopy);
    // 初始化 GGUF 上下文所需的参数，不分配额外内存，上下文指针初始化为 nullptr
    gguf_init_params initParams = {.no_alloc = true, .ctx = nullptr};
    // 根据模型文件路径和初始化参数创建 GGUF 上下文
    gguf_context *ggufContext = gguf_init_from_file(modelPathCStr, initParams);
    env->ReleaseStringUTFChars(modelPath, modelPathCStr);
    return reinterpret_cast<jlong>(ggufContext);
}

/**
 * @brief 获取 GGUF 模型的上下文大小。
 *
 * 该函数通过 JNI 从 Java 层调用，根据给定的 GGUF 上下文本地句柄，
 * 查找并返回模型的上下文大小。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param nativeHandle 指向 GGUF 上下文的本地句柄。
 * @return 模型的上下文大小，若查找失败则返回 -1。
 */
extern "C" JNIEXPORT jlong JNICALL
Java_com_stephen_llamacppbridge_GgufFileReader_getContextSize(JNIEnv *env, jobject thiz,
                                                              jlong nativeHandle) {
    // 将 jlong 类型的本地句柄转换为 gguf_context 指针
    gguf_context *ggufContext = reinterpret_cast<gguf_context *>(nativeHandle);
    // 查找模型架构信息对应的键 ID
    int64_t architectureKeyId = gguf_find_key(ggufContext, "general.architecture");
    // 若未找到架构信息键 ID，返回 -1
    if (architectureKeyId == -1)
        return -1;
    // 获取模型架构信息
    std::string architecture = gguf_get_val_str(ggufContext, architectureKeyId);
    // 构建上下文长度信息对应的键名
    std::string contextLengthKey = architecture + ".context_length";
    // 查找上下文长度信息对应的键 ID
    int64_t contextLengthKeyId = gguf_find_key(ggufContext, contextLengthKey.c_str());
    // 若未找到上下文长度信息键 ID，返回 -1
    if (contextLengthKeyId == -1)
        return -1;
    uint32_t contextLength = gguf_get_val_u32(ggufContext, contextLengthKeyId);
    return contextLength;
}

/**
 * @brief 获取 GGUF 模型的聊天模板。
 *
 * 该函数通过 JNI 从 Java 层调用，根据给定的 GGUF 上下文本地句柄，
 * 查找并返回模型的聊天模板。
 *
 * @param env JNI 环境指针，用于与 Java 虚拟机交互。
 * @param thiz 调用该本地方法的 Java 对象引用。
 * @param nativeHandle 指向 GGUF 上下文的本地句柄。
 * @return 包含聊天模板的 Java 字符串对象，若未找到则返回空字符串对应的 Java 字符串。
 */
extern "C" JNIEXPORT jstring JNICALL
Java_com_stephen_llamacppbridge_GgufFileReader_getChatTemplate(JNIEnv *env, jobject thiz,
                                                               jlong nativeHandle) {
    // 将 jlong 类型的本地句柄转换为 gguf_context 指针
    gguf_context *ggufContext = reinterpret_cast<gguf_context *>(nativeHandle);
    // 查找聊天模板信息对应的键 ID
    int64_t chatTemplateKeyId = gguf_find_key(ggufContext, "tokenizer.chat_template");
    // 存储聊天模板的字符串
    std::string chatTemplate;
    // 若未找到聊天模板信息键 ID，将聊天模板设为空字符串
    if (chatTemplateKeyId == -1) {
        chatTemplate = "";
    } else {
        // 若找到聊天模板信息键 ID，获取聊天模板信息
        chatTemplate = gguf_get_val_str(ggufContext, chatTemplateKeyId);
    }
    // 将 C++ 字符串转换为 Java 字符串并返回
    return env->NewStringUTF(chatTemplate.c_str());
}