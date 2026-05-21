package com.junjie.rag.controller;

/**
 * @Title: KnowledgeController
 * @Author junjie
 * @Package com.junjie.rag.controller
 * @Date 2025/2/8 20:35
 * @description: 知识库
 */

import com.alibaba.fastjson2.JSON;
import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ErrorCode;
import com.junjie.rag.common.ResultUtils;
import com.junjie.rag.entity.AliOssFile;
import com.junjie.rag.pojo.dto.QueryFileDTO;
import com.junjie.rag.service.AliOssFileService;
import com.junjie.rag.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "KnowledgeController", description = "知识库管理接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/knowledge")
public class KnowledgeController {

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AliOssUtil aliOssUtil;

    @Autowired
    private TokenTextSplitter tokenTextSplitter;



    @Autowired
    private AliOssFileService aliOssFileService;
    /**
     * 上传附件接口
     *
     *  1. 提供不同的分片策略
     *  2. 分片后的预览
     * @param
     * @return
     * @throws IOException
     */

    @Operation(summary = "upload", description = "上传附件接口")
    @PostMapping(value = "file/upload", headers = "content-type=multipart/form-data")
    public BaseResponse upload(@RequestParam("file") List<MultipartFile> files) {

        if (files.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请选择文件");
        }

        for (MultipartFile file : files) {
            try {
                String originalFilename = file.getOriginalFilename();
                // 取文件名的后缀
                assert originalFilename != null;
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFileName = UUID.randomUUID() + fileExtension;
                // 上传文件到oss
                String url = aliOssUtil.upload(file.getBytes(), newFileName);

                // 向量化
                // 读取文件
                Resource resource = file.getResource();
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> documents = reader.read();
                // 分词
                List<Document> splitDocuments = tokenTextSplitter.apply(documents);
                // 向量化，自动调用向量模型的向量化方法
                vectorStore.add(splitDocuments);
                // 持久化数据库
                long currtime = System.currentTimeMillis();
                aliOssFileService.save(
                        AliOssFile.builder()
                                .fileName(originalFilename)
                                .url(url)
                                .vectorId(JSON.toJSONString(splitDocuments.stream().map(Document::getId).collect(Collectors.toList())))
                                .createTime(new Date(currtime))
                                .updateTime(new Date(currtime))
                                .build()
                );
            } catch (IOException e) {
                log.error("上传文件失败", e);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "上传文件失败");
            } catch (Exception e) {
                log.error("上传文件失败", e);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "向量化失败");
            }


        }
        return ResultUtils.success("文件上传成功");
    }


    @Operation(summary = "contents",description = "文件查询")
    @GetMapping("/contents")
    public BaseResponse queryFiles(QueryFileDTO request){
        if(request.getPage() == null || request.getPageSize() == null){
            return ResultUtils.error(ErrorCode.PARAMS_ERROR,"page 或 pageSize为空");
        }
        return aliOssFileService.queryPage(request);
    }

    @Operation(summary = "delete",description = "文件删除")
    @DeleteMapping("/delete")
    public BaseResponse deleteFiles(@RequestParam List<Long> ids){
        return aliOssFileService.deleteFiles(ids);
    }


    @Operation(summary = "download",description = "文件下载")
    @GetMapping("/download")
    public BaseResponse downloadFiles(@RequestParam List<Long> ids){
        return aliOssFileService.downloadFiles(ids);
    }





}
