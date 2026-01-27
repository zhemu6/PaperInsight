package com.zhemu.paperinsight.model.vo;

import cn.hutool.core.bean.BeanUtil;
import com.zhemu.paperinsight.model.entity.PaperInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 论文 VO 类
 * @author lushihao
 */
@Data
public class PaperVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String authors;
    private String abstractInfo;
    private String keywords;
    private String cosUrl;
    private String coverUrl;
    private Long folderId;
    private Long userId;
    private Integer isPublic;
    private LocalDateTime publishDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static PaperVO objToVo(PaperInfo paperInfo) {
        if (paperInfo == null) {
            return null;
        }
        PaperVO paperVO = new PaperVO();
        BeanUtil.copyProperties(paperInfo, paperVO);
        return paperVO;
    }
}
