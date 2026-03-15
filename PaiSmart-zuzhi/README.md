PaiSmart是一个企业级的 AI 知识库管理系统，采用检索增强生成（RAG）技术，提供智能文档处理和检索能力。

核心技术栈包括 ElasticSearch、Kafka、WebSocket、Spring Security、Docker、MySQL 和 Redis。

它的目标是帮助企业和个人更高效地管理和利用知识库中的信息，支持多租户架构，允许用户通过自然语言查询知识库，并获得基于自身文档的 AI 生成响应。


系统允许用户：

- 上传和管理各种类型的文档
- 自动处理和索引文档内容
- 使用自然语言查询知识库
- 接收基于自身文档的 AI 生成响应

用到的技术栈包括

+ 框架 : Spring Boot 3.4.2 (Java 17)
+ 数据库 : MySQL 8.0
+ 缓存 : Redis
+ 搜索引擎 : Elasticsearch 8.10.0
+ 消息队列 : Apache Kafka
+ 文件存储 : MinIO
+ 文档解析 : Apache Tika
+ 安全认证 : Spring Security + JWT
+ 实时通信 : WebSocket
+ 响应式编程 : WebFlux

后端的整体项目结构：

```bash
src/main/java/com/yizhaoqi/smartpai/
├── SmartPaiApplication.java      # 主应用程序入口
├── client/                       # 外部API客户端
├── config/                       # 配置类
├── consumer/                     # Kafka消费者
├── controller/                   # REST API端点
├── entity/                       # 数据实体
├── exception/                    # 自定义异常
├── handler/                      # WebSocket处理器
├── model/                        # 领域模型
├── repository/                   # 数据访问层
├── service/                      # 业务逻辑
└── utils/                        # 工具类
```
前端的整体项目结构：

```bash
frontend/
├── packages/           # 可重用模块
├── public/             # 静态资源
├── src/                # 主应用程序代码
│   ├── assets/         # SVG图标，图片
│   ├── components/     # Vue组件
│   ├── layouts/        # 页面布局
│   ├── router/         # 路由配置
│   ├── service/        # API集成
│   ├── store/          # 状态管理
│   ├── views/          # 页面组件
│   └── ...            # 其他工具和配置
└── ...               # 构建配置文件
```

## 核心功能

### 知识库管理

PaiSmart提供了完整的文档上传与解析功能，支持文件分片上传和断点续传，并支持标签进行组织管理。文档可以是公开的，也可以是私有的，并且可以与特定的组织标签关联，以便更好地进行权限分类。

![PaiSmart文档处理](https://cdn.tobebetterjavaer.com/stutymore/README-20250730102808.png)

### AI驱动的RAG实现

PaiSmart的核心是 RAG 实现：

![PaiSmart聊天交互](https://cdn.tobebetterjavaer.com/stutymore/README-20250730102837.png)

- 将上传的文档进行语义分块
- 调用豆包 Embedding 模型为每个文本块生成高维向量
- 将向量存储到 ElasticSearch 以支持语义搜索和关键词搜索
- 可以根据用户的查询检索相关文档
- 为 LLM 提供完整的上下文，从而生成更准确、基于文档的响应内容

### 企业级多租户

PaiSmart通过组织标签支持多租户架构。每个用户可以创建或加入一个或多个组织，每个组织可以拥有独立的知识库和文档管理。这样，企业可以在同一系统中管理多个团队或部门的知识库，而无需担心数据混淆或权限问题。

![PaiSmart的安全架构](https://cdn.tobebetterjavaer.com/stutymore/README-20250730103118.png)

### 实时通信

系统采用 WebSocket 技术，提供用户与 AI 系统之间的实时交互，支持响应式聊天界面，便于知识检索和 AI 互动。

## 前置环境

- Java 17
- Maven 3.8.6 或更高版本
- Node.js 18.20.0 或更高版本
- pnpm 8.7.0 或更高版本
- MySQL 8.0
- Elasticsearch 8.10.0
- MinIO 8.5.12
- Kafka 3.2.1
- Redis 7.0.11
- Docker（可选，用于运行 Redis、MinIO、Elasticsearch 和 Kafka 等服务）
