package com.junjie.rag.service;

import com.junjie.rag.common.PageResult;
import com.junjie.rag.entity.WordFrequency;
import com.baomidou.mybatisplus.extension.service.IService;
import com.junjie.rag.pojo.dto.WordFrequencyPageQueryDTO;

/**
* @author junjie
* @description 针对表【word_frequency】的数据库操作Service
* @createDate 2025-03-06 15:56:07
*/
public interface WordFrequencyService extends IService<WordFrequency> {

    PageResult pageQuery(WordFrequencyPageQueryDTO queryDTO);
}
