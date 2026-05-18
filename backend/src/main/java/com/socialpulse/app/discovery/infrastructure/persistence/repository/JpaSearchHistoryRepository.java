package com.socialpulse.app.discovery.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.discovery.infrastructure.persistence.entity.SearchHistoryEntity;

@Repository
public interface JpaSearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Long> {

    Optional<SearchHistoryEntity> findByUserIdAndKeyword(Long userId, String keyword);

    List<SearchHistoryEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserId(Long userId);

    int countByUserId(Long userId);

    @Query("SELECT sh FROM SearchHistoryEntity sh WHERE sh.userId = :userId ORDER BY sh.updatedAt ASC LIMIT 1")
    Optional<SearchHistoryEntity> findOldestByUserId(@Param("userId") Long userId);

    Optional<SearchHistoryEntity> findByIdAndUserId(Long id, Long userId);
}
