package com.example.elibrary.infrastructure.persistence.repository;

import com.example.elibrary.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query("select o from OutboxEventEntity o where o.publishedAt is null order by o.id asc")
    List<OutboxEventEntity> findUnpublished(org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("update OutboxEventEntity o set o.publishedAt = :now where o.eventId in :eventIds")
    int markPublished(@Param("eventIds") Collection<String> eventIds, @Param("now") Instant now);
}
