package com.junjie.rag.controller;

import com.junjie.rag.common.ApplicationConstant;
import com.junjie.rag.common.BaseResponse;
import com.junjie.rag.common.ResultUtils;
import com.junjie.rag.entity.LlmModelConfig;
import com.junjie.rag.service.LlmModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Tag(name = "LlmModelController", description = "LLM 模型管理")
@Slf4j
@RestController
@RequestMapping(ApplicationConstant.API_VERSION + "/model")
public class LlmModelController {

    @Autowired
    private LlmModelConfigService llmModelConfigService;

    @Operation(summary = "模型列表")
    @GetMapping("/list")
    public BaseResponse<List<LlmModelConfig>> list() {
        List<LlmModelConfig> list = llmModelConfigService.lambdaQuery()
                .orderByAsc(LlmModelConfig::getSortOrder).list();
        return ResultUtils.success(list);
    }

    @Operation(summary = "切换模型")
    @PostMapping("/switch")
    public BaseResponse switchTo(@RequestParam Long id) {
        boolean ok = llmModelConfigService.switchTo(id);
        return ok ? ResultUtils.success("切换成功") : ResultUtils.error("模型不存在");
    }

    @Operation(summary = "当前模型 ID")
    @GetMapping("/current")
    public BaseResponse<Long> current() {
        return ResultUtils.success(llmModelConfigService.getCurrentModelId());
    }

    @Operation(summary = "新增自定义模型")
    @PostMapping("/add")
    public BaseResponse add(@RequestBody LlmModelConfig config) {
        config.setIsDefault(false);
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        boolean ok = llmModelConfigService.save(config);
        return ok ? ResultUtils.success(config) : ResultUtils.error("添加失败");
    }

    @Operation(summary = "更新模型")
    @PutMapping("/update")
    public BaseResponse update(@RequestBody LlmModelConfig config) {
        config.setUpdateTime(new Date());
        boolean ok = llmModelConfigService.updateById(config);
        return ok ? ResultUtils.success("更新成功") : ResultUtils.error("更新失败");
    }

    @Operation(summary = "删除模型")
    @DeleteMapping("/{id}")
    public BaseResponse delete(@PathVariable Long id) {
        LlmModelConfig config = llmModelConfigService.getById(id);
        if (config == null) return ResultUtils.error("模型不存在");
        if (Boolean.TRUE.equals(config.getIsDefault())) {
            return ResultUtils.error("预置模型不可删除");
        }
        boolean ok = llmModelConfigService.removeById(id);
        return ok ? ResultUtils.success("删除成功") : ResultUtils.error("删除失败");
    }
}
