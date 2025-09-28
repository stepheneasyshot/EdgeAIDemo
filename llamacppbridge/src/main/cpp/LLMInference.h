#pragma once
#include "llama.h"
#include <jni.h>
#include "common.h"
#include <string>
#include <vector>

/**
 * @class LLMInference
 * @brief 该类用于管理大语言模型（LLM）的推理过程，包括模型加载、聊天消息处理、推理循环等功能。
 */
class LLMInference {
    // llama.cpp 特定类型的成员变量
    /// llama 上下文指针，用于管理模型的运行时状态
    llama_context *_ctx;
    /// llama 模型指针，指向加载的大语言模型
    llama_model *_model;
    /// llama 采样器指针，用于从模型输出中采样生成下一个 token
    llama_sampler *_sampler;
    /// 当前采样得到的 llama token
    llama_token _currToken;
    /// llama 批处理结构，用于批量处理输入 token
    llama_batch *_batch;

    // 存储聊天中用户/助手消息的容器
    /// 存储聊天过程中用户和助手的消息列表
    std::vector<llama_chat_message> _messages;
    // 存储将聊天模板应用到 `_messages` 中所有消息后生成的字符串
    /// 存储应用聊天模板后的格式化消息
    std::vector<char> _formattedMessages;
    // 存储追加到 `_messages` 中的最后一个查询的 token
    /// 存储最后一次查询的 token 列表
    std::vector<llama_token> _promptTokens;
    /// 上一次格式化消息的长度
    int _prevLen = 0;
    /// 聊天模板字符串指针
    const char *_chatTemplate;

    // 存储给定查询的完整响应
    /// 存储当前查询的完整响应内容
    std::string _response;
    /// 缓存响应的 token 片段
    std::string _cacheResponseTokens;
    // 是否在 `_messages` 中缓存先前的消息
    /// 是否存储聊天历史消息的标志
    bool _storeChats;

    // 响应生成指标
    /// 记录响应生成所花费的总时间（微秒）
    int64_t _responseGenerationTime = 0;
    /// 记录生成的 token 总数
    long _responseNumTokens = 0;

    // 对话过程中消耗的上下文窗口长度
    /// 记录对话过程中已使用的上下文大小
    int _nCtxUsed = 0;

    /**
     * @brief 检查输入的字符串是否为有效的 UTF-8 编码。
     *
     * @param response 待检查的字符串。
     * @return bool 若为有效 UTF-8 编码返回 true，否则返回 false。
     */
    bool _isValidUtf8(const char *response);

public:
    /**
     * @brief 加载大语言模型并初始化相关参数。
     *
     * @param modelPath 模型文件的路径。
     * @param minP 采样时的最小概率阈值。
     * @param temperature 采样时的温度参数。
     * @param storeChats 是否存储聊天记录。
     * @param contextSize 模型的上下文大小。
     * @param chatTemplate 聊天模板字符串。
     * @param nThreads 推理时使用的线程数量。
     * @param useMmap 是否使用内存映射加载模型。
     * @param useMlock 是否使用内存锁定。
     */
    void loadModel(const char *modelPath, float minP, float temperature, bool storeChats,
                   long contextSize,
                   const char *chatTemplate, int nThreads, bool useMmap, bool useMlock);

    /**
     * @brief 向聊天消息列表中添加一条消息。
     *
     * @param message 消息内容。
     * @param role 消息角色，如 "user" 或 "assistant"。
     */
    void addChatMessage(const char *message, const char *role);

    /**
     * @brief 获取响应生成的速度。
     *
     * 计算方式为生成的 token 数量除以生成所用的总时间（秒）。
     *
     * @return float 响应生成速度，单位为 token/秒。
     */
    float getResponseGenerationTime() const;

    /**
     * @brief 获取当前已使用的上下文大小。
     *
     * @return int 当前已使用的上下文大小。
     */
    int getContextSizeUsed() const;

    /**
     * @brief 开始完成任务，处理用户输入并准备推理。
     *
     * @param query 用户输入的查询内容。
     */
    void startCompletion(const char *query);

    /**
     * @brief 完成任务的循环函数，进行模型推理和响应生成。
     *
     * @return std::string 生成的有效 UTF-8 词块，若无效则返回空字符串，若生成结束则返回 "[EOG]"。
     */
    std::string completionLoop();

    /**
     * @brief 停止完成任务，处理收尾工作。
     */
    void stopCompletion();

    /**
     * @brief 析构函数，释放类实例占用的资源。
     */
    ~LLMInference();
};