package com.topos.admin.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线用户记录表 {@code sys_user_online}。
 * <p>部分列名为历史脚本中的驼峰写法（如 {@code sessionId}），与默认下划线映射不一致处已用 {@code @TableField} 标注。</p>
 */
@Data
@TableName("sys_user_online")
public class SysUserOnline implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("sessionId")
    private String sessionId;

    private String loginName;
    private String deptName;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private String status;

    @TableField("start_timestamp")
    private LocalDateTime startTimestamp;

    @TableField("last_access_time")
    private LocalDateTime lastAccessTime;

    private Integer expireTime;

    @TableField("session_data")
    private byte[] sessionData;
}
