package com.junjie.rag.controller;

/**
 * @Title: KnowledgeController
 * @Author junjie
 * @Package com.junjie.rag.controller
 * @Date 2025/2/8 20:35
 * @description: 知识库
 */

import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ErrorCode;
import com.junjie.rag.common.ResultUtils;
import com.junjie.rag.pojo.dto.QueryFileDTO;
import com.junjie.rag.service.impl.AsyncDocumentProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "KnowledgeController", description = "知识库管理接口")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/knowledge")
public class KnowledgeController {

    @Autowired
    private AsyncDocumentProcessor asyncDocumentProcessor;

    @Autowired
    private com.junjie.rag.service.AliOssFileService aliOssFileService;

    @Operation(summary = "upload", description = "上传附件接口（异步处理）")
    @PostMapping(value = "file/upload", headers = "content-type=multipart/form-data")
    public BaseResponse upload(@RequestParam("file") List<MultipartFile> files) {
        if (files.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请选择文件");
        }

        for (MultipartFile file : files) {
            try {
                if (file.getOriginalFilename() == null) continue;
                byte[] bytes = file.getBytes(); // 在请求结束前读取字节
                asyncDocumentProcessor.processFile(file.getOriginalFilename(), bytes);
            } catch (java.io.IOException e) {
                log.error("读取文件失败", e);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "读取文件失败: " + file.getOriginalFilename());
            }
        }
        return ResultUtils.success("文件上传成功，后台正在处理中");
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
