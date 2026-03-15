package com.yizhaoqi.smartpai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 嵌入向量生成客户端
@Component
public class EmbeddingClient {

    @Value("${embedding.api.model}")
    private String modelId;
    
    @Value("${embedding.api.batch-size:100}")
    private int batchSize;

    @Value("${embedding.api.dimension:2048}")
    private int dimension;
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingClient.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingClient(WebClient embeddingWebClient, ObjectMapper objectMapper) {
        this.webClient = embeddingWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用通义千问 API 生成向量
     * @param texts 输入文本列表
     * @return 对应的向量列表
     */
    public List<float[]> embed(List<String> texts) {
        try {
            logger.info("开始生成向量，文本数量: {}", texts.size());
            
            List<float[]> all = new ArrayList<>(texts.size());
            for (int start = 0; start < texts.size(); start += batchSize) {
                int end = Math.min(start + batchSize, texts.size());
                List<String> sub = texts.subList(start, end);
                logger.debug("调用向量 API, 批次: {}-{} (size={})", start, end - 1, sub.size());
                String response = callApiOnce(sub);
                all.addAll(parseVectors(response));
            }
            logger.info("成功生成向量，总数量: {}", all.size());
            return all;
        } catch (Exception e) {
            logger.error("调用向量化 API 失败: {}", e.getMessage(), e);
            throw new RuntimeException("向量生成失败", e);
        }
    }

    private String callApiOnce(List<String> batch) {
        // 清洗文本：去除NUL字符等控制字符，过滤空文本，防止API返回400
        List<String> cleanBatch = batch.stream()
                .map(t -> t == null ? "" : t.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ").trim())
                .filter(t -> !t.isEmpty())
                .toList();
        if (cleanBatch.isEmpty()) {
            throw new RuntimeException("批次中所有文本均为空，跳过向量化");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("input", cleanBatch);
        // OpenAI 兼容模式：dimensions 是顶级字段（复数形式）
        requestBody.put("dimensions", dimension);

        logger.info("Embedding API 请求体: model={}, inputSize={}, dimensions={}", modelId, cleanBatch.size(), dimension);

        return webClient.post()
                .uri("/embeddings")
                .bodyValue(requestBody)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class);
                    } else {
                        return response.bodyToMono(String.class).flatMap(errorBody -> {
                            logger.error("Embedding API 错误, 状态码: {}, 响应体: {}", response.statusCode(), errorBody);
                            return reactor.core.publisher.Mono.error(
                                new RuntimeException("Embedding API " + response.statusCode() + ": " + errorBody));
                        });
                    }
                })
                .block(Duration.ofSeconds(30));
    }

    private List<float[]> parseVectors(String response) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(response);
        JsonNode data = jsonNode.get("data");  // 兼容模式下使用data字段
        if (data == null || !data.isArray()) {
            throw new RuntimeException("API 响应格式错误: data 字段不存在或不是数组");
        }
        
        List<float[]> vectors = new ArrayList<>();
        for (JsonNode item : data) {
            JsonNode embedding = item.get("embedding");
            if (embedding != null && embedding.isArray()) {
                float[] vector = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vector[i] = (float) embedding.get(i).asDouble();
                }
                vectors.add(vector);
            }
        }
        return vectors;
    }
}
