package com.zhemu.paperinsight.model.dto.announcement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnnouncementUpdateRequest {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
