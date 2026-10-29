package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.infrastructure.persistence.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberJpaRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByEmail(String email);
}
