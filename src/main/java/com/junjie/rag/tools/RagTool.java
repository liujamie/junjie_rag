package com.junjie.rag.tools;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RagTool {

//    @Autowired
//    ChatModel chatModel;

//    @Tool(description = "判断用户的查询是否涉及统计数据、求和、计数、平均值等聚合操作")
//    public String aggregate(@ToolParam(description = "用户的提问") String  question) {
//
//        return "test result";
//    }

    @Tool(description = "当用户的查询包含特定人名刘梦杰时使用此工具来获取相关资料")
    public String addInfo(@ToolParam(description = "用户的完整提问内容", required = true) String  question) {
        System.out.println("工具被调用，question: " + question);
        return "增加年龄，曾经任职公司的信息";
    }
}
