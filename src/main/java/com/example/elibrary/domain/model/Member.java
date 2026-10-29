package com.example.elibrary.domain.model;

import lombok.Getter;

/**
 * 图书馆账号。只保留够用的那点身份信息：一是能认证，二是能判断这个人还能不能借书。
 */
@Getter
public class Member {

    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private MemberRole role;
    private MemberStatus status;
    private Long version;

    private Member() {
    }

    public Member(String name, String email, String passwordHash, MemberRole role) {
        this(null, name, email, passwordHash, role, MemberStatus.ACTIVE, null);
    }

    private Member(Long id, String name, String email, String passwordHash, MemberRole role,
                   MemberStatus status, Long version) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
        this.version = version;
    }

    public static Member restore(Long id, String name, String email, String passwordHash,
                                 MemberRole role, MemberStatus status, Long version) {
        return new Member(id, name, email, passwordHash, role, status, version);
    }

    /** Lets the persistence adapter write back a generated identifier. */
    public void assignIdForNewPersistence(Long generatedId) {
        this.id = generatedId;
    }

    public boolean canBorrow() {
        return status == MemberStatus.ACTIVE;
    }

    public boolean suspended() {
        return status == MemberStatus.SUSPENDED;
    }

    public boolean isLibrarian() {
        return role == MemberRole.LIBRARIAN;
    }
}
