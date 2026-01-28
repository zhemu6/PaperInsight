package com.zhemu.paperinsight.controller;

import cn.hutool.json.JSONObject;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;
import com.zhemu.paperinsight.service.JournalService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 期刊等级查询接口
 * 基于 EasyScholar 开放 API
 *
 * @author lushihao
 */
@RestController
@RequestMapping("/journal")
@Slf4j
public class JournalController {

    @Resource
    private JournalService journalService;

    /**
     * 查询期刊等级
     *
     * @param name 期刊名称
     * @return 期刊等级信息
     */
    @GetMapping("/rank")
    public BaseResponse<JSONObject> getJournalRank(@RequestParam("name") String name) {
        return ResultUtils.success(journalService.getJournalRank(name));
    }
}
