package com.zhemu.paperinsight.model.dto.announcement;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnnouncementReadRequest {

    @NotNull
    private Long id;
}
