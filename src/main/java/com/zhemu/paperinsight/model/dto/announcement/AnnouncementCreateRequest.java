package com.zhemu.paperinsight.model.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
