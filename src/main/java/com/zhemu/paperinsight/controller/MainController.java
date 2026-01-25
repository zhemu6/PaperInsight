package com.zhemu.paperinsight.controller;

import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试项目和接口文档是否能够正常生成
 * @author: lushihao
 * @version: 1.0
 * create:   2026-01-23   22:19
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}

