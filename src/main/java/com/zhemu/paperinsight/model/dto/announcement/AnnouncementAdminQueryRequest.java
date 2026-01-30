package com.zhemu.paperinsight.model.dto.announcement;

import com.zhemu.paperinsight.common.PageRequest;
import lombok.Data;

@Data
public class AnnouncementAdminQueryRequest extends PageRequest {

    /**
     * draft/published
     */
    private String status;
}
