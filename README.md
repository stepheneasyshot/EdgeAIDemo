# EdgeAIDemo

探索端侧模型集成方案，四个Demo功能
* 使用 `llama.cpp` 运行 `gguf` 格式的端侧模型
* 使用 LiteRT 框架运行端侧模型，通过MediePipe Tasks API调用
* pixel 9 设备安装AI Core，跨进程通信调用端侧的Gemini Nano模型。
* 端侧RAG知识库，使用Embeder模型，将知识库转换为向量表示，存储在端侧。使用Cross-Encoder模型，对用户查询进行编码，与知识库中的向量表示进行相似度计算，返回Top-K个最相关的文档。

Demo首页：

![](/resources/blogs_edge_ai_demo.png)

llama.cpp 集成LLM：

![](/resources/blogs_ai_smoll_model.png)