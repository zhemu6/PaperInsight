package com.zhemu.paperinsight.model.dto.folder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author lushihao
 */
@Data
public class FolderUpdateRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id必须大于0")
    private Long id;

    /**
     * 文件夹名称
     */
    @NotNull(message = "更新文件夹名称不能为空")
    @Size(max = 128, message = "文件夹名称过长")
    private String name;

}
