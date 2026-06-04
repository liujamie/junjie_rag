package com.junjie.rag.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName(value ="conversation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
