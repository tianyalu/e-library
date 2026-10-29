package com.example.elibrary.application.port.out;

import com.example.elibrary.domain.model.Member;

import java.util.Optional;

/** Members. */
public interface MemberRepositoryPort {

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

    Member save(Member member);
}
