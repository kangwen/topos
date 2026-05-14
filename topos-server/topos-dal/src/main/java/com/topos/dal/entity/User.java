package com.topos.dal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String phone;
    private String passwordHash;
    private String nickname;
    private String avatarUrl;
    /** 1 正常 0 禁用 */
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime pwdUpdatedAt;
}
