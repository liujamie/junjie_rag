# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build (skip tests since there are none)
mvn clean install -DskipTests

# Run
mvn spring-boot:run

# Run with dev profile (default via application.yml)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package
mvn clean package -DskipTests
```

## Dependencies

- **Java 17**, Spring Boot 3.4.2, Spring AI 1.0.0
- Milvus vector database (port 19530), MySQL (port 3306), Redis (port 6379)
- Required environment variables: `DASHSCOPE_API_KEY`, `alioss.access-key-id`, `alioss.access-key-secret`
- Default server port: **8989**

## Project Architecture

Standard Spring Boot layered architecture:

```
controller/    → REST API endpoints (AiRagController, KnowledgeController, ChatController, etc.)
service/       → Business logic interfaces + impl/
mapper/        → MyBatis Plus mappers (XML in resources/mapper/)
entity/        → JPA entities (User, SensitiveWord, LogInfo, AliOssFile, etc.)
common/        → Cross-cutting: BaseResponse, ErrorCode, JwtTokenUserInterceptor, ResultUtils
config/        → Spring config: ApplicationConfig (WebMvcConfigurer + beans), OssConfiguration, RedisConfig
constant/      → Constants: JwtClaimsConstant, MessageConstant, StatusConstant
context/       → BaseContext (ThreadLocal for current userId)
exception/     → GlobalExceptionHandler + custom exceptions
utils/         → AliOssUtil, JwtUtil, SearchUtils
aop/           → LoggingAspect (@Loggable annotation)
scheduled/     → TaskJobScheduled (scheduled tasks)
tools/         → RagTool (Spring AI @Tool definitions)
```

## API Structure

All endpoints under `/api/v1` (defined in `ApplicationConstant.API_VERSION`):

| Base Path | Controller | Description |
|-----------|-----------|-------------|
| `/api/v1/ai` | AiRagController | RAG knowledge base Q&A (SSE streaming) |
| `/api/v1/chat` | ChatController | Standard chat (SSE streaming) |
| `/api/v1/knowledge` | KnowledgeController | Knowledge file CRUD + upload |
| `/api/v1/user` | UserController | User registration/login/CRUD |
| `/api/v1/draw` | DrawImageController | AI image generation |
| `/api/v1/sensitive` | SensitiveWordController | Sensitive word CRUD |
| `/api/v1/category` | SensitiveCategoryController | Sensitive word category CRUD |
| `/api/v1/frequency` | WordFrequencyController | Word frequency statistics |
| `/api/v1/log` | LogInfoController | Operation log CRUD |

## Auth & Security

- JWT token auth via `JwtTokenUserInterceptor` — all endpoints except `/login`, `/register`, Swagger paths
- Token in `Authorization: Bearer <token>` header
- Sensitive word filtering on AI chat inputs
- `@Loggable` annotation on controller methods for operation audit logging (AOP aspect)

## Key AI Patterns

- **RAG flow**: `TikaDocumentReader` → `TokenTextSplitter` → `VectorStore.add()` (upload); `QuestionAnswerAdvisor` with similarity search (query)
- **Chat memory**: `PromptChatMemoryAdvisor` + `ChatMemory` — conversation history per userId
- **Vector store**: Milvus with COSINE metric, IVF_FLAT index, 1536-dim embeddings
- **AI models**: DashScope (Alibaba Cloud) via Spring AI Alibaba
- **Image generation**: `DrawImageController` via `ImageModel`

## Configuration Files

- `src/main/resources/application.yml` — main config (datasource, redis, jwt, oss, vector store, dashscope)
- `src/main/resources/application-dev.yml` — dev overrides
- `docker-compose.yml` — pgvector (unused), MinIO (object storage), know-hub-backend
