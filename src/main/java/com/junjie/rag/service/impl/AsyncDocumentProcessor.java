package com.junjie.rag.service.impl;

import com.alibaba.fastjson2.JSON;
import com.junjie.rag.entity.AliOssFile;
import com.junjie.rag.service.AliOssFileService;
import com.junjie.rag.service.Bm25IndexService;
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

    @Autowired
    private Bm25IndexService bm25IndexService;

    @Async
    public void processFile(String originalFilename, byte[] fileBytes) {
        try {
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.error("文件名称为空");
                return;
            }

            // 覆盖更新：同名文件已存在时，删旧记录再重新处理
            AliOssFile existing = aliOssFileService.lambdaQuery()
                    .eq(AliOssFile::getFileName, originalFilename)
                    .one();
            if (existing != null) {
                log.info("检测到同名文件，覆盖更新: {}", originalFilename);
                List<String> oldVectorIds = JSON.parseArray(existing.getVectorId(), String.class);
                if (oldVectorIds != null && !oldVectorIds.isEmpty()) {
                    vectorStore.delete(oldVectorIds);
                    bm25IndexService.deleteDocuments(oldVectorIds);
                }
                aliOssUtil.deleteOss(existing.getUrl());
                aliOssFileService.removeById(existing.getId());
                log.info("旧记录已清理: {}", originalFilename);
            }

            ByteArrayResource resource = new ByteArrayResource(fileBytes);
            List<Document> documents;
            if (originalFilename.toLowerCase().endsWith(".pdf")) {
                log.info("开始解析PDF: {}", originalFilename);
                documents = new PagePdfDocumentReader(resource).read();
            } else {
                log.info("开始解析文档: {}", originalFilename);
                documents = new TikaDocumentReader(resource).read();
            }
            log.info("文档解析完成, 共{}个文档, 总字符数: {}",
                    documents.size(),
                    documents.stream().mapToInt(d -> d.getText().length()).sum());

            List<Document> splitDocuments = tokenTextSplitter.apply(documents);
            log.info("文档分块完成, 共{}个块", splitDocuments.size());

            if (splitDocuments.isEmpty()) {
                log.warn("文档分块后为空，跳过处理: {}", originalFilename);
                return;
            }

            log.info("开始向量化...");
            vectorStore.add(splitDocuments);
            log.info("向量化完成");

            // 同步到 BM25 全文索引
            try {
                java.util.List<java.util.Map.Entry<String, String>> bm25Docs = new java.util.ArrayList<>();
                for (Document doc : splitDocuments) {
                    bm25Docs.add(new java.util.AbstractMap.SimpleEntry<>(doc.getId(), doc.getText()));
                }
                bm25IndexService.addDocuments(bm25Docs);
                log.info("BM25 索引完成: {}条", bm25Docs.size());
            } catch (Exception e) {
                log.warn("BM25 索引失败（不影响主流程）: {}", e.getMessage());
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID() + fileExtension;
            String url = aliOssUtil.upload(fileBytes, newFileName);
            log.info("OSS上传完成: {}", url);

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
