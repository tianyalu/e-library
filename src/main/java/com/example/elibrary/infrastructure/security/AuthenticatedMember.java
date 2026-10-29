package com.example.elibrary.infrastructure.security;

import com.example.elibrary.domain.model.MemberRole;

import java.util.Objects;

/**
 * 当前请求的调用方。
 *
 * <p>只带 ID 不带实体，一来 principal 轻量，二来保证每次请求都是对着库里当前状态做判断，
 * 而不是拿登录那一刻的快照。
 *
 * @param memberId 会员数字 ID，也是所有查询判断归属的依据
 * @param role     只用于接口级这种粗粒度判断
 */
public record AuthenticatedMember(Long memberId, MemberRole role) {

    public AuthenticatedMember {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(role, "role");
    }

    public boolean isLibrarian() {
        return role == MemberRole.LIBRARIAN;
    }
}
