package com.zhemu.paperinsight.model.dto.folder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件夹添加请求请求
 *
 * @author: lushihao
 * @version: 1.0
 *           create: 2025-07-27 17:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FolderAddRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件夹名称
     */
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 128, message = "文件夹名称过长")
    private String name;

    /**
     * 父文件夹ID (0代表根目录)
     */
    private Long parentId;

}
