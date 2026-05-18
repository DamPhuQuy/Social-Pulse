package com.socialpulse.app.discovery.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.discovery.domain.model.SearchHistory;
import com.socialpulse.app.discovery.domain.repository.SearchHistoryRepository;
import com.socialpulse.app.discovery.infrastructure.persistence.mapper.SearchHistoryPersistenceMapper;
import com.socialpulse.app.discovery.infrastructure.persistence.repository.JpaSearchHistoryRepository;

@Repository
public class SearchHistoryRepositoryAdapter implements SearchHistoryRepository {

    private final JpaSearchHistoryRepository jpaRepository;
    private final SearchHistoryPersistenceMapper mapper;

    public SearchHistoryRepositoryAdapter(
            JpaSearchHistoryRepository jpaRepository,
            SearchHistoryPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SearchHistory> findByUserIdAndKeyword(Long userId, String keyword) {
        return jpaRepository.findByUserIdAndKeyword(userId, keyword)
                .map(mapper::toDomain);
    }

    @Override
    public List<SearchHistory> findByUserIdOrderByUpdatedAtDesc(Long userId) {
        return jpaRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SearchHistory save(SearchHistory searchHistory) {
        var entity = mapper.toEntity(searchHistory);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public int countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public Optional<SearchHistory> findOldestByUserId(Long userId) {
        return jpaRepository.findOldestByUserId(userId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SearchHistory> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId)
                .map(mapper::toDomain);
    }
}
