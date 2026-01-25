package com.zhemu.paperinsight.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author lushihao
 * @description 针对表【paper_info(论文信息表)】的数据库操作Mapper
 * @createDate 2026-01-23 23:24:48
 * @Entity generator.domain.PaperInfo
 */
public interface PaperInfoMapper extends BaseMapper<PaperInfo> {

    /**
     * 查询逻辑删除的论文
     */
    List<PaperInfo> selectDeletedPapers(@Param("userId") Long userId);

    /**
     * 还原论文
     */
    int restorePaper(@Param("id") Long id, @Param("folderId") Long folderId);

    /**
     * 物理删除论文
     */
    int physicalDeletePaper(@Param("id") Long id);
}
