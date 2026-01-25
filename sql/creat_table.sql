-- 建库建表语句
-- 1. 建库
CREATE DATABASE paper_insight;

-- 2. 建表
-- 2.1 用户表
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_account`  varchar(256) NOT NULL COMMENT '账号',
    `user_password` varchar(512) NOT NULL COMMENT '密码',
    `user_name`     varchar(256)          DEFAULT NULL COMMENT '用户昵称',
    `user_avatar`   varchar(1024)         DEFAULT NULL COMMENT '用户头像',
    `user_profile`  varchar(512)          DEFAULT NULL COMMENT '用户简介',
    `user_role`      varchar(256) default 'user'            not null comment '用户角色：user/admin',
    `user_status`   tinyint      NOT NULL DEFAULT '0' COMMENT '状态 0-正常 1-禁用 2-待审核',
    `email`         varchar(256)          DEFAULT NULL COMMENT '邮箱',
    `create_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`     tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userAccount` (`user_account`),
    KEY `idx_userName` (`user_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

-- 2. 文件夹表 (folder)
CREATE TABLE IF NOT EXISTS `folder`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        varchar(256) NOT NULL COMMENT '文件夹名称',
    `user_id`     bigint       NOT NULL COMMENT '创建人ID',
    `parent_id`   bigint       NOT NULL DEFAULT '0' COMMENT '父文件夹ID (0代表根目录)',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`   tinyint      NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`user_id`),
    KEY `idx_parentId` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文件夹表';

-- 3. 论文信息表 (paper_info)
CREATE TABLE IF NOT EXISTS `paper_info`
(
    `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`        varchar(512)  NOT NULL COMMENT '论文标题',
    `authors`      varchar(1024)          DEFAULT NULL COMMENT '作者列表',
    `abstract_info`     text COMMENT '摘要',
    `keywords`     varchar(512)           DEFAULT NULL COMMENT '关键词',
    `cos_url`      varchar(1024) NOT NULL COMMENT 'COS存储地址',
    `cover_url`    varchar(1024) DEFAULT NULL COMMENT '封面图片地址',
    `folder_id`    bigint                 DEFAULT '0' COMMENT '所属文件夹ID',
    `user_id`      bigint        NOT NULL COMMENT '上传用户ID',
    `is_public`    tinyint       NOT NULL DEFAULT '0' COMMENT '是否公开 (0-私有 1-公开)',
    `publish_date` date                   DEFAULT NULL COMMENT '发表日期',
    `create_time`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`    tinyint       NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`user_id`),
    KEY `idx_folderId` (`folder_id`),
    KEY `idx_title` (`title`(128))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='论文信息表';

-- 4. 论文智能分析表 (paper_insight)
CREATE TABLE IF NOT EXISTS `paper_insight`
(
    `id`                bigint   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `paper_id`          bigint   NOT NULL COMMENT '论文ID',
    `summary_markdown`  text COMMENT 'AI生成的摘要总结(Markdown)',
    `innovation_points` text COMMENT '创新点',
    `methods`           text COMMENT '方法论',
    `score`             int               DEFAULT NULL COMMENT '评分 (0-100)',
    `create_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_delete`         tinyint  NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paperId` (`paper_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='论文AI分析结果表';
