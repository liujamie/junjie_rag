package com.junjie.rag.service.impl;

import com.alibaba.fastjson2.JSON;
import com.junjie.rag.entity.AliOssFile;
import com.junjie.rag.service.AliOssFileService;
import com.junjie.rag.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AsyncDocumentProcessor {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private TokenTextSplitter tokenTextSplitter;

    @Autowired
    private AliOssFileService aliOssFileService;

    @Async
    public void processFile(String originalFilename, byte[] fileBytes) {
        try {
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.error("文件名称为空");
                return;
            }
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID() + fileExtension;
            String url = aliOssUtil.upload(fileBytes, newFileName);
            log.info("OSS上传完成: {}", url);

            ByteArrayResource resource = new ByteArrayResource(fileBytes);
            List<Document> documents;
            if (originalFilename.toLowerCase().endsWith(".pdf")) {
                log.info("开始解析PDF: {}", originalFilename);
                documents = new PagePdfDocumentReader(resource).read();
            } else {
                log.info("开始解析文档: {}", originalFilename);
                documents = new TikaDocumentReader(resource).read();
            }
            log.info("文档解析完成, 共{}个文档", documents.size());

            List<Document> splitDocuments = tokenTextSplitter.apply(documents);
            log.info("文档分块完成, 共{}个块", splitDocuments.size());

            log.info("开始向量化...");
            vectorStore.add(splitDocuments);
            log.info("向量化完成");

            long currtime = System.currentTimeMillis();
            aliOssFileService.save(
                    AliOssFile.builder()
                            .fileName(originalFilename)
                            .url(url)
                            .vectorId(JSON.toJSONString(
                                    splitDocuments.stream().map(Document::getId).collect(Collectors.toList())))
                            .createTime(new Date(currtime))
                            .updateTime(new Date(currtime))
                            .build()
            );
            log.info("文件处理完成: {}", originalFilename);
        } catch (Exception e) {
            log.error("文件处理失败: {}", originalFilename, e);
        }
    }
}
