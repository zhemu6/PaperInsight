package com.zhemu.paperinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhemu.paperinsight.model.entity.Folder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lushihao
 * @description 针对表【folder(文件夹表)】的数据库操作Mapper
 * @createDate 2026-01-23 23:26:42
 * @Entity generator.domain.Folder
 */
public interface FolderMapper extends BaseMapper<Folder> {

    /**
     * 查询逻辑删除的文件夹
     */
    List<Folder> selectDeletedFolders(@Param("userId") Long userId);

    /**
     * 还原文件夹 (逻辑恢复)
     */
    int restoreFolder(@Param("id") Long id);

    /**
     * 物理删除文件夹
     */
    int physicalDeleteFolder(@Param("id") Long id);
}
