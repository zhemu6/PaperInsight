package com.zhemu.paperinsight.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.service.JournalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 期刊服务实现类
 *
 * @author lushihao
 */
@Service
@Slf4j
public class JournalServiceImpl implements JournalService {

    private static final String EASYSCHOLAR_API = "https://www.easyscholar.cc/open/getPublicationRank";
    private static final String REDIS_KEY_PREFIX = "journal:rank:";

    @Value("${easyscholar.secret-key}")
    private String secretKey;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public JSONObject getJournalRank(String name) {
        // 1. 校验参数
        ThrowUtils.throwIf(name == null || name.isBlank(), ErrorCode.PARAMS_ERROR, "期刊名称不能为空");
        String trimmedName = name.trim();

        // 2. 查询缓存
        String cacheKey = REDIS_KEY_PREFIX + trimmedName;
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.parseObj(cachedValue);
        }

        // 3. 调用外部 API
        try {
            String encodedName = URLEncoder.encode(trimmedName, StandardCharsets.UTF_8);
            String url = EASYSCHOLAR_API + "?secretKey=" + secretKey + "&publicationName=" + encodedName;
            // 10s timeout
            String response = HttpUtil.get(url, 10000);
            JSONObject result = JSONUtil.parseObj(response);

            int code = result.getInt("code", -1);
            if (code != 200) {
                String msg = result.getStr("msg", "查询失败");
                log.warn("EasyScholar API error: {}", msg);
                ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "查询失败: " + msg);
            }

            // 4. 获取数据并缓存
            JSONObject data = result.getJSONObject("data");
            if (data != null) {
                // 缓存 24 小时 (期刊等级变动不频繁)
                stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(data), 24, TimeUnit.HOURS);
            }
            return data;

        } catch (Exception e) {
            log.error("查询期刊等级失败: {}", e.getMessage());
            if (e instanceof com.zhemu.paperinsight.exception.BusinessException) {
                throw e;
            }
            throw new com.zhemu.paperinsight.exception.BusinessException(ErrorCode.SYSTEM_ERROR, "外部接口调用失败");
        }
    }
}
