package com.zhemu.paperinsight.model.dto.recycle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 回收站请求
 * @author lushihao
 */
@Data
@Builder
public class RecycleRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 论文 ID
     */
    @Min(value = 1, message = "论文ID错误")
    @NotBlank(message = "论文ID不能为空")
    private Long paperId;

    /**
     * 文件夹ID
     */
    private Long folderId;
}
