package com.example.elibrary.infrastructure.persistence.adapter;

import com.example.elibrary.application.port.out.MemberRepositoryPort;
import com.example.elibrary.domain.model.Member;
import com.example.elibrary.infrastructure.persistence.entity.MemberEntity;
import com.example.elibrary.infrastructure.persistence.mapper.PersistenceMapper;
import com.example.elibrary.infrastructure.persistence.repository.MemberJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MemberRepositoryAdapter implements MemberRepositoryPort {

    private final MemberJpaRepository jpa;

    public MemberRepositoryAdapter(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Member> findById(Long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return jpa.findByEmail(email).map(PersistenceMapper::toDomain);
    }

    @Override
    public Member save(Member member) {
        MemberEntity entity = member.getId() == null
                ? new MemberEntity()
                : jpa.findById(member.getId()).orElseGet(MemberEntity::new);
        PersistenceMapper.apply(member, entity);
        MemberEntity saved = jpa.saveAndFlush(entity);
        member.assignIdForNewPersistence(saved.getId());
        return PersistenceMapper.toDomain(saved);
    }
}
