package com.junjie.rag.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ErrorCode;
import com.junjie.rag.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import com.junjie.rag.entity.AliOssFile;
import com.junjie.rag.mapper.AliOssFileMapper;
import com.junjie.rag.pojo.dto.QueryFileDTO;
import com.junjie.rag.service.AliOssFileService;
import com.junjie.rag.utils.AliOssUtil;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author junjie
* @description 针对表【ali_oss_file】的数据库操作Service实现
* @createDate 2025-02-08 20:51:33
*/
@Slf4j
@Service
public class AliOssFileServiceImpl extends ServiceImpl<AliOssFileMapper, AliOssFile>
    implements AliOssFileService{

    @Autowired
    private AliOssFileMapper aliOssFileMapper;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AliOssUtil aliOssUtil;


    /**
     * 查询文件
     * @param request
     * @return
     */
    @Override
    public BaseResponse queryPage(QueryFileDTO request) {
        PageHelper.startPage(request.getPage() + 1, request.getPageSize());
        List<AliOssFile> list = aliOssFileMapper.findListByFileName(request.getFileName());
        PageInfo<AliOssFile> pageInfo = new PageInfo<>(list);
        IPage<AliOssFile> page = new Page<>(request.getPage(), request.getPageSize(), pageInfo.getTotal());
        page.setRecords(list);
        return ResultUtils.success(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse deleteFiles(List<Long> ids) {
        List<AliOssFile> aliOssFiles = aliOssFileMapper.selectByIds(ids);
        if (ids.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请选择文件");
        }
        int count = aliOssFileMapper.deleteBatchIds(ids);
        if (count == 0) {
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "删除失败");
        }
        for (AliOssFile aliOssFile : aliOssFiles) {
            try {
                List<String> vectorIds = JSON.parseArray(aliOssFile.getVectorId(), String.class);
                if (vectorIds != null && !vectorIds.isEmpty()) {
                    vectorStore.delete(vectorIds);
                }
            } catch (Exception e) {
                log.warn("向量删除失败（可能已不存在）, fileId={}: {}", aliOssFile.getId(), e.getMessage());
            }
            aliOssUtil.deleteOss(aliOssFile.getUrl());
        }

        return ResultUtils.success("成功删除"+ count + "个文件");
    }

    @Override
    public BaseResponse downloadFiles(List<Long> ids) {
        List<AliOssFile> aliOssFiles = aliOssFileMapper.selectByIds(ids);
        if (aliOssFiles.isEmpty()) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR, "请选择文件");
        }
        List<Map<String, String>> fileList = aliOssFiles.stream()
                .map(file -> {
                    String objectName = file.getUrl().substring(file.getUrl().lastIndexOf("/") + 1);
                    String signedUrl = aliOssUtil.generateSignedUrl(objectName, file.getFileName());
                    Map<String, String> item = new HashMap<>();
                    item.put("url", signedUrl != null ? signedUrl : file.getUrl());
                    item.put("name", file.getFileName());
                    return item;
                })
                .collect(Collectors.toList());
        return ResultUtils.success(fileList.size() == 1 ? fileList.get(0) : fileList);
    }

}




