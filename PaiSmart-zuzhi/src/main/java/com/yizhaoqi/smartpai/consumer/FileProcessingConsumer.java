package com.yizhaoqi.smartpai.consumer;

import com.yizhaoqi.smartpai.config.KafkaConfig;
import com.yizhaoqi.smartpai.model.FileProcessingTask;
import com.yizhaoqi.smartpai.service.ParseService;
import com.yizhaoqi.smartpai.service.VectorizationService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
@Slf4j
public class FileProcessingConsumer {

    private final ParseService parseService;
    private final VectorizationService vectorizationService;
    @Autowired
    private KafkaConfig kafkaConfig;
    @Autowired
    private MinioClient minioClient;

    public FileProcessingConsumer(ParseService parseService, VectorizationService vectorizationService) {
        this.parseService = parseService;
        this.vectorizationService = vectorizationService;
    }

    @KafkaListener(topics = "#{kafkaConfig.getFileProcessingTopic()}", groupId = "#{kafkaConfig.getFileProcessingGroupId()}")
    public void processTask(FileProcessingTask task) {
        log.info("Received task: {}", task);
        log.info("文件权限信息: userId={}, orgTag={}, isPublic={}", 
                task.getUserId(), task.getOrgTag(), task.isPublic());
                
        InputStream fileStream = null;
        try {
            // 优先使用 minioObjectName 直连 MinIO 获取流，避免预签名 URL 过期或 URL 解码乱码
            fileStream = downloadFromMinio(task);
            if (fileStream == null) {
                throw new IOException("无法获取文件流，minioObjectName=" + task.getMinioObjectName()
                        + ", filePath=" + task.getFilePath());
            }

            // 强制转换为可缓存流
            if (!fileStream.markSupported()) {
                fileStream = new BufferedInputStream(fileStream);
            }

            // 解析文件
            parseService.parseAndSave(task.getFileMd5(), fileStream, task.getFileName(),
                    task.getUserId(), task.getOrgTag(), task.isPublic());
            log.info("文件解析完成，fileMd5: {}", task.getFileMd5());

            // 向量化处理
            vectorizationService.vectorize(task.getFileMd5(), 
                    task.getUserId(), task.getOrgTag(), task.isPublic());
            log.info("向量化完成，fileMd5: {}", task.getFileMd5());
        } catch (Exception e) {
            log.error("Error processing task: {}", task, e);
            throw new RuntimeException("Error processing task", e);
        } finally {
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (IOException e) {
                    log.error("Error closing file stream", e);
                }
            }
        }
    }

    /**
     * 优先通过 MinioClient 直接获取文件流（不走预签名 URL），
     * 如果 minioObjectName 为空则降级为 HTTP 下载。
     */
    private InputStream downloadFromMinio(FileProcessingTask task) {
        // 1. 直接走 MinioClient（推荐，无编码问题）
        String objectName = task.getMinioObjectName();
        if (objectName == null || objectName.isBlank()) {
            // 兼容旧消息：根据 fileName 推断
            objectName = "merged/" + task.getFileName();
        }
        try {
            log.info("通过 MinioClient 获取文件流: bucket=uploads, object={}", objectName);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket("uploads")
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.warn("MinioClient 获取文件流失败: object={}, 错误: {}", objectName, e.getMessage());
        }

        // 2. 降级：通过 filePath（预签名 URL）下载
        String filePath = task.getFilePath();
        if (filePath != null && !filePath.isBlank()) {
            try {
                log.warn("降级使用预签名 URL 下载: {}", filePath);
                java.net.URL url = new java.net.URL(filePath);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(180000);
                connection.setRequestProperty("User-Agent", "SmartPAI-FileProcessor/1.0");
                int code = connection.getResponseCode();
                if (code == java.net.HttpURLConnection.HTTP_OK) {
                    return connection.getInputStream();
                }
                log.error("HTTP 下载失败，响应码: {}", code);
            } catch (Exception e) {
                log.error("预签名 URL 下载失败: {}", e.getMessage(), e);
            }
        }

        return null;
    }
}
