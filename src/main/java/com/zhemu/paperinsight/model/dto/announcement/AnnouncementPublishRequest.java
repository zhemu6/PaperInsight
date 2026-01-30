package com.zhemu.paperinsight.model.dto.announcement;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnnouncementPublishRequest {

    @NotNull
    private Long id;

    @NotNull
    private Boolean published;
}
