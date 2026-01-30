package com.zhemu.paperinsight.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 通知类型枚举
 * @author lushihao
 */
@Getter
public enum NotificationTypeEnum {

    PaperAnalysisSuccess("论文分析完成", "paper_analysis_success"),
    PaperAnalysisFailed("论文分析失败", "paper_analysis_failed");

    private final String desc;
    private final String value;

    NotificationTypeEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    public static NotificationTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (NotificationTypeEnum anEnum : NotificationTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
