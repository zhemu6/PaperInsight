package com.zhemu.paperinsight.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.ObjectMetadata;
import com.zhemu.paperinsight.annotation.AuthCheck;
import com.zhemu.paperinsight.common.BaseResponse;
import com.zhemu.paperinsight.common.ResultUtils;

import com.zhemu.paperinsight.exception.BusinessException;
import com.zhemu.paperinsight.exception.ErrorCode;
import com.zhemu.paperinsight.exception.ThrowUtils;
import com.zhemu.paperinsight.manager.CosManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件接口
 *
 * @author lushihao
 */
@RestController
@RequestMapping("/file")
@Slf4j
@RequiredArgsConstructor
public class FileController {

    private final CosManager cosManager;

    /**
     * 文件上传
     *
     * @param multipartFile 上传的文件对象
     * @param request       HTTP请求对象
     * @return 文件访问URL
     */
    @PostMapping("/upload")
    @AuthCheck
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {
        String filename = multipartFile.getOriginalFilename();
        ThrowUtils.throwIf(StrUtil.isBlank(filename),ErrorCode.PARAMS_ERROR,"文件名为空");
        long size = multipartFile.getSize();
        // 20MB
        if (size > 20 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 20M");
        }

        // 校验文件后缀
        String suffix = FileUtil.getSuffix(filename);
        if (!StrUtil.equalsAnyIgnoreCase(suffix, "png", "jpg", "jpeg", "webp", "pdf", "doc", "docx", "ppt", "pptx",
                "xls", "xlsx")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件类型");
        }

        // 生成唯一文件名
        String uuid = IdUtil.simpleUUID();
        String key = String.format("paper/%s.%s", uuid, suffix);

        try {
            // 上传
            InputStream inputStream = multipartFile.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(size);
            metadata.setContentType(multipartFile.getContentType());
            cosManager.putObject(key, inputStream, metadata);

            // 获取访问地址
            String accessUrl = cosManager.getAccessUrl(key);
            return ResultUtils.success(accessUrl);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }
}
