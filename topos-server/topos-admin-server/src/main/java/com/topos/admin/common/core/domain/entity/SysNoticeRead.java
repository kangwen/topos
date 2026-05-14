package com.topos.admin.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告已读记录表 {@code sys_notice_read}。
 */
@Data
@TableName("sys_notice_read")
public class SysNoticeRead implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "read_id", type = IdType.AUTO)
    private Long readId;

    private Integer noticeId;
    private Long userId;
    private LocalDateTime readTime;
}
