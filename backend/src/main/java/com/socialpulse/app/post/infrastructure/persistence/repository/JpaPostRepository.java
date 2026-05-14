package com.socialpulse.app.post.infrastructure.persistence.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;

@Repository
public interface JpaPostRepository extends JpaRepository<PostEntity, Long> {

    // get posts by user id
    Page<PostEntity> findByUserId(Long userId, Pageable pageable);

    List<PostEntity> findAllByIdIn(Set<Long> ids);

    boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type);

    @Query("UPDATE PostEntity p SET p.shareCount = p.shareCount + :delta WHERE p.id = :postId")
    void updateShareCount(@Param("postId") Long postId, @Param("delta") Long delta);

    long countByUserId(Long userId);

    @Query("SELECT p.user.id, COUNT(p) FROM PostEntity p WHERE p.user.id IN :userIds GROUP BY p.user.id")
    List<Object[]> countByUserIds(@Param("userIds") java.util.Set<Long> userIds);
}
