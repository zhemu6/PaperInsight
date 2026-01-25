package com.zhemu.paperinsight.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.zhemu.paperinsight.model.entity.Folder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class FolderVO implements Serializable {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 文件夹名称
     */
    private String name;

    /**
     * 父文件夹ID
     */
    private Long parentId;

    /**
     * 创建人ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;

    /**
     * 包装类转对象
     *
     * @param folder
     * @return
     */
    public static FolderVO objToVo(Folder folder) {
        if (folder == null) {
            return null;
        }
        FolderVO folderVO = new FolderVO();
        BeanUtil.copyProperties(folder, folderVO);
        return folderVO;
    }
}
