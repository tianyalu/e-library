package com.example.elibrary.domain.model;

/** 角色。故意只留两个：业务规则就是照这两个角色写的。 */
public enum MemberRole {
    /** 普通读者。 */
    MEMBER,
    /** 后台管理员：能维护目录、能看到所有借阅。 */
    LIBRARIAN
}
