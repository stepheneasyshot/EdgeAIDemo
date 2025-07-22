#include "LLMInference.h"
#include "llama.h"
#include "gguf.h"
#include <android/log.h>
#include <cstring>
#include <iostream>

// 定义日志标签，用于在 Android 日志系统中标识本模块的日志
#define TAG "[SmolLMAndroid-Cpp]"
// 定义信息日志宏，方便打印信息级别的日志
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
// 定义错误日志宏，方便打印错误级别的日志
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// 函数声明：对输入文本进行分词处理
// vocab: 指向 llama 词表的指针
// text: 待分词的字符串
// add_special: 是否添加特殊标记
// parse_special: 是否解析特殊标记，默认值为 false
std::vector<llama_token> common_tokenize(
        const struct llama_vocab *vocab,
        const std::string &text,
        bool add_special,
        bool parse_special = false);

// 函数声明：将 llama 标记转换为对应的词块
// ctx: 指向 llama 上下文的指针
// token: llama 标记
// special: 是否为特殊标记，默认值为 true
std::string common_token_to_piece(
        const struct llama_context *ctx,
        llama_token token,
        bool special = true);

/**
 * @brief 加载 LLM 模型并初始化相关参数
 *
 * 该函数负责根据传入的参数加载 LLM 模型，创建模型上下文、采样器等，
 * 并对这些组件进行初始化配置。
 *
 * @param model_path 模型文件的路径
 * @param minP 采样时使用的最小概率阈值
 * @param temperature 采样时使用的温度参数
 * @param storeChats 是否存储聊天记录
 * @param contextSize 模型的上下文大小
 * @param chatTemplate 聊天模板字符串
 * @param nThreads 推理时使用的线程数量
 * @param useMmap 是否使用内存映射来加载模型
 * @param useMlock 是否使用内存锁定
 */
void
LLMInference::loadModel(const char *model_path, float minP, float temperature, bool storeChats,
                        long contextSize,
                        const char *chatTemplate, int nThreads, bool useMmap, bool useMlock) {
    // 打印加载模型时使用的各项参数，方便调试和日志记录
    LOGi("loading model with"
         "\n\tmodel_path = %s"
         "\n\tminP = %f"
         "\n\ttemperature = %f"
         "\n\tstoreChats = %d"
         "\n\tcontextSize = %li"
         "\n\tchatTemplate = %s"
         "\n\tnThreads = %d"
         "\n\tuseMmap = %d"
         "\n\tuseMlock = %d",
         model_path, minP, temperature, storeChats, contextSize, chatTemplate, nThreads, useMmap,
         useMlock);

    // 创建一个 llama_model_params 实例，并使用默认参数初始化
    llama_model_params model_params = llama_model_default_params();
    // 设置是否使用内存映射加载模型
    model_params.use_mmap = useMmap;
    // 设置是否使用内存锁定
    model_params.use_mlock = useMlock;
    // 从指定路径加载模型
    _model = llama_model_load_from_file(model_path, model_params);

    // 检查模型是否加载成功
    if (!_model) {
        // 若加载失败，打印错误日志
        LOGe("failed to load model from %s", model_path);
        // 抛出运行时错误异常
        throw std::runtime_error("loadModel() failed");
    }

    // 创建一个 llama_context_params 实例，并使用默认参数初始化
    llama_context_params ctx_params = llama_context_default_params();
    // 设置模型的上下文大小
    ctx_params.n_ctx = contextSize;
    // 设置推理时使用的线程数量
    ctx_params.n_threads = nThreads;
    // 禁用性能指标统计
    ctx_params.no_perf = true;
    // 基于加载的模型初始化 llama 上下文
    _ctx = llama_init_from_model(_model, ctx_params);

    // 检查上下文是否创建成功
    if (!_ctx) {
        // 若创建失败，打印错误日志
        LOGe("llama_new_context_with_model() returned null)");
        // 抛出运行时错误异常
        throw std::runtime_error("llama_new_context_with_model() returned null");
    }

    // 初始化采样器参数，使用默认参数
    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    // 禁用采样器的性能指标统计
    sampler_params.no_perf = true;
    // 初始化采样器链
    _sampler = llama_sampler_chain_init(sampler_params);
    // 向采样器链中添加最小概率采样器
    llama_sampler_chain_add(_sampler, llama_sampler_init_min_p(minP, 1));
    // 向采样器链中添加温度采样器
    llama_sampler_chain_add(_sampler, llama_sampler_init_temp(temperature));
    // 向采样器链中添加分布采样器，使用默认种子
    llama_sampler_chain_add(_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    // 初始化格式化消息缓冲区，大小为模型的上下文大小
    _formattedMessages = std::vector<char>(llama_n_ctx(_ctx));
    // 清空消息列表
    _messages.clear();
    // 复制聊天模板字符串
    _chatTemplate = strdup(chatTemplate);
    // 设置是否存储聊天记录
    this->_storeChats = storeChats;
}

/**
 * @brief 向聊天消息列表中添加一条消息
 *
 * @param message 消息内容
 * @param role 消息角色，如 "user" 或 "assistant"
 */
void
LLMInference::addChatMessage(const char *message, const char *role) {
    // 将消息角色和内容复制后添加到消息列表中
    _messages.push_back({strdup(role), strdup(message)});
}

/**
 * @brief 获取响应生成的速度
 *
 * 计算方式为生成的 token 数量除以生成所用的总时间（秒）
 *
 * @return float 响应生成速度，单位为 token/秒
 */
float
LLMInference::getResponseGenerationTime() const {
    return (float) _responseNumTokens / (_responseGenerationTime / 1e6);
}

/**
 * @brief 获取当前已使用的上下文大小
 *
 * @return int 当前已使用的上下文大小
 */
int
LLMInference::getContextSizeUsed() const {
    return _nCtxUsed;
}

/**
 * @brief 开始完成任务，处理用户输入并准备推理
 *
 * @param query 用户输入的查询内容
 */
void
LLMInference::startCompletion(const char *query) {
    // 如果不存储聊天记录，则重置相关状态
    if (!_storeChats) {
        _prevLen = 0;
        _formattedMessages.clear();
        _formattedMessages = std::vector<char>(llama_n_ctx(_ctx));
    }
    // 重置响应生成时间和生成的 token 数量
    _responseGenerationTime = 0;
    _responseNumTokens = 0;
    // 将用户查询添加到聊天消息列表中
    addChatMessage(query, "user");
    // 应用聊天模板，格式化聊天消息
    int newLen = llama_chat_apply_template(_chatTemplate, _messages.data(), _messages.size(), true,
                                           _formattedMessages.data(), _formattedMessages.size());
    // 检查格式化后的消息长度是否超过缓冲区大小
    if (newLen > (int) _formattedMessages.size()) {
        // 若超过，则调整缓冲区大小
        _formattedMessages.resize(newLen);
        // 重新应用聊天模板
        newLen = llama_chat_apply_template(_chatTemplate, _messages.data(), _messages.size(), true,
                                           _formattedMessages.data(), _formattedMessages.size());
    }
    // 检查聊天模板应用是否失败
    if (newLen < 0) {
        // 若失败，抛出运行时错误异常
        throw std::runtime_error(
                "llama_chat_apply_template() in LLMInference::startCompletion() failed");
    }
    // 提取格式化后的提示信息
    std::string prompt(_formattedMessages.begin() + _prevLen, _formattedMessages.begin() + newLen);
    // 对提示信息进行分词处理
    _promptTokens = common_tokenize(llama_model_get_vocab(_model), prompt, true, true);

    // 创建一个 llama_batch 实例，包含单个序列
    // 详情可参考 llama_batch_init 函数
    _batch.token = _promptTokens.data();
    _batch.n_tokens = _promptTokens.size();
}

// 代码来源：
// https://github.com/ggerganov/llama.cpp/blob/master/examples/llama.android/llama/src/main/cpp/llama-android.cpp#L38
/**
 * @brief 检查输入的字符串是否为有效的 UTF-8 编码
 *
 * @param response 待检查的字符串
 * @return bool 若为有效 UTF-8 编码返回 true，否则返回 false
 */
bool
LLMInference::_isValidUtf8(const char *response) {
    // 若输入为空指针，认为是有效的 UTF-8 编码
    if (!response) {
        return true;
    }
    // 将输入字符串转换为无符号字符指针
    const unsigned char *bytes = (const unsigned char *) response;
    int num;
    // 遍历字符串中的每个字节
    while (*bytes != 0x00) {
        if ((*bytes & 0x80) == 0x00) {
            // U+0000 到 U+007F 的单字节编码
            num = 1;
        } else if ((*bytes & 0xE0) == 0xC0) {
            // U+0080 到 U+07FF 的双字节编码
            num = 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            // U+0800 到 U+FFFF 的三字节编码
            num = 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            // U+10000 到 U+10FFFF 的四字节编码
            num = 4;
        } else {
            // 不符合 UTF-8 编码规则，返回 false
            return false;
        }

        bytes += 1;
        // 检查后续的续字节是否符合 UTF-8 编码规则
        for (int i = 1; i < num; ++i) {
            if ((*bytes & 0xC0) != 0x80) {
                // 续字节不符合规则，返回 false
                return false;
            }
            bytes += 1;
        }
    }
    // 所有字节都符合 UTF-8 编码规则，返回 true
    return true;
}

/**
 * @brief 完成任务的循环函数，进行模型推理和响应生成
 *
 * @return std::string 生成的有效 UTF-8 词块，若无效则返回空字符串，若生成结束则返回 "[EOG]"
 */
std::string
LLMInference::completionLoop() {
    // 检查输入的长度是否超过模型的上下文大小
    uint32_t contextSize = llama_n_ctx(_ctx);
    _nCtxUsed = llama_kv_self_used_cells(_ctx);
    if (_nCtxUsed + _batch.n_tokens > contextSize) {
        // 若超过，抛出运行时错误异常
        throw std::runtime_error("context size reached");
    }

    // 记录模型推理开始时间
    auto start = ggml_time_us();
    // 运行模型进行解码
    if (llama_decode(_ctx, _batch) < 0) {
        // 若解码失败，抛出运行时错误异常
        throw std::runtime_error("llama_decode() failed");
    }

    // 采样一个 token，并检查是否为生成结束标记
    _currToken = llama_sampler_sample(_sampler, _ctx, -1);
    if (llama_vocab_is_eog(llama_model_get_vocab(_model), _currToken)) {
        // 若为生成结束标记，将响应添加到聊天消息列表中
        addChatMessage(strdup(_response.data()), "assistant");
        // 清空响应缓冲区
        _response.clear();
        // 返回生成结束标记
        return "[EOG]";
    }
    // 将采样的 token 转换为对应的词块
    std::string piece = common_token_to_piece(_ctx, _currToken, true);
    // 打印转换后的词块信息
    LOGi("common_token_to_piece: %s", piece.c_str());
    // 记录模型推理结束时间
    auto end = ggml_time_us();
    // 累加响应生成时间
    _responseGenerationTime += (end - start);
    // 累加生成的 token 数量
    _responseNumTokens += 1;
    // 将生成的词块添加到缓存中
    _cacheResponseTokens += piece;

    // 重新初始化 batch，使用新生成的 token
    _batch.token = &_currToken;
    _batch.n_tokens = 1;

    // 检查缓存中的词块是否为有效的 UTF-8 编码
    if (_isValidUtf8(_cacheResponseTokens.c_str())) {
        // 若有效，将其添加到响应缓冲区中
        _response += _cacheResponseTokens;
        std::string valid_utf8_piece = _cacheResponseTokens;
        // 清空缓存
        _cacheResponseTokens.clear();
        // 返回有效的 UTF-8 词块
        return valid_utf8_piece;
    }

    // 若无效，返回空字符串
    return "";
}

/**
 * @brief 停止完成任务，处理收尾工作
 */
void
LLMInference::stopCompletion() {
    // 如果存储聊天记录，将响应添加到聊天消息列表中
    if (_storeChats) {
        addChatMessage(_response.c_str(), "assistant");
    }
    // 清空响应缓冲区
    _response.clear();
    // 获取模型的聊天模板
    const char *tmpl = llama_model_chat_template(_model, nullptr);
    // 应用聊天模板，计算格式化后的消息长度
    _prevLen = llama_chat_apply_template(tmpl, _messages.data(), _messages.size(), false, nullptr,
                                         0);
    // 检查聊天模板应用是否失败
    if (_prevLen < 0) {
        // 若失败，抛出运行时错误异常
        throw std::runtime_error(
                "llama_chat_apply_template() in LLMInference::stopCompletion() failed");
    }
}

/**
 * @brief LLMInference 类的析构函数，释放相关资源
 */
LLMInference::~LLMInference() {
    // 打印析构信息
    LOGi("deallocating LLMInference instance");
    // 释放聊天消息列表中动态分配的内存
    for (llama_chat_message &message: _messages) {
        free(const_cast<char *>(message.role));
        free(const_cast<char *>(message.content));
    }
    // 释放聊天模板动态分配的内存
    free(const_cast<char *>(_chatTemplate));
    // 释放采样器资源
    llama_sampler_free(_sampler);
    // 释放 llama 上下文资源
    llama_free(_ctx);
    // 释放 llama 模型资源
    llama_model_free(_model);
}