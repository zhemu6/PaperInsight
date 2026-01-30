package com.zhemu.paperinsight.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 对话事件枚举类
 * @author lushihao
 */

@Getter
public enum ChatEventTypeEnum {

    TEXT("TEXT"),
    THINKING("THINKING"),
    TOOL_USE("TOOL_USE"),
    TOOL_RESULT("TOOL_RESULT"),
    ERROR("ERROR"),
    COMPLETE("COMPLETE"),
    INTERRUPTED("INTERRUPTED");

    private final String value;

    ChatEventTypeEnum(String value) {
        this.value = value;
    }

    public static ChatEventTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatEventTypeEnum anEnum : ChatEventTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
