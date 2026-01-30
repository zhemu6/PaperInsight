package com.zhemu.paperinsight.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 公告状态枚举
 * @author lushihao
 */
@Getter
public enum AnnouncementStatusEnum {

    Draft("草稿", "draft"),
    Published("已发布", "published");

    private final String desc;
    private final String value;

    AnnouncementStatusEnum(String desc, String value) {
        this.desc = desc;
        this.value = value;
    }

    public static AnnouncementStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AnnouncementStatusEnum anEnum : AnnouncementStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
