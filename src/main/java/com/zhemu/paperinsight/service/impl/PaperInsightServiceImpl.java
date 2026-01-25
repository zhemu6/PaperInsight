package com.zhemu.paperinsight.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhemu.paperinsight.model.entity.PaperInsight;
import com.zhemu.paperinsight.service.PaperInsightService;
import com.zhemu.paperinsight.mapper.PaperInsightMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
* @author lushihao
* @description 针对表【paper_insight(论文AI分析结果表)】的数据库操作Service实现
* @createDate 2026-01-23 23:25:50
*/
@Service
@RequiredArgsConstructor
public class PaperInsightServiceImpl extends ServiceImpl<PaperInsightMapper, PaperInsight>
    implements PaperInsightService{

}




