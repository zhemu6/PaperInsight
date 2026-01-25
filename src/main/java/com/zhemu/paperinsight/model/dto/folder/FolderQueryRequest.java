package com.zhemu.paperinsight.model.dto.folder;

import com.zhemu.paperinsight.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询请求
 * @author lushihao
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FolderQueryRequest extends PageRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    private Long id;

    /**
     * 父文件夹ID
     */
    private Long parentId;

    /**
     * 文件夹名称（模糊查询）
     */
    private String name;

}
