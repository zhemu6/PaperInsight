package com.zhemu.paperinsight.service;

import cn.hutool.json.JSONObject;

/**
 * 期刊服务接口
 *
 * @author lushihao
 */
public interface JournalService {

    /**
     * 查询期刊等级信息
     *
     * @param name 期刊名称
     * @return 期刊等级数据
     */
    JSONObject getJournalRank(String name);
}
