package com.socialpulse.app.post;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;
import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReactPostConcurrencyTest {

    @Autowired
    private JpaPostRepository jpaPostRepository;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private Long testPostId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        UserEntity author = UserEntity.builder()
                .username("dev_author_" + System.currentTimeMillis())
                .email("author_" + System.currentTimeMillis() + "@devpulse.tech")
                .passwordHash("hashed_pw")
                .status(UserStatus.ACTIVE)
                .verification(VerificationStatus.VERIFIED)
                .build();
        author = jpaUserRepository.save(author);

        PostEntity post = PostEntity.builder()
                .user(author)
                .title("High-Concurrency Architecture in Java 21")
                .content("High-Concurrency Architecture in Java 21")
                .type(PostType.ORIGINAL)
                .privacy(Privacy.PUBLIC)
                .upvoteCount(0L)
                .downvoteCount(0L)
                .hotScore(0.0)
                .build();
        post = jpaPostRepository.save(post);
        testPostId = post.getId();
    }

    @Test
    @DisplayName("100 concurrent threads upvoting the same post produces exactly 100 upvotes with 0% lost updates")
    void concurrentUpvotes_zeroLostUpdates() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // All threads wait to blast simultaneously
                    transactionTemplate.execute(status -> {
                        jpaPostRepository.updateUpvoteCountAtomic(testPostId, 1L);
                        return null;
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait until all 100 threads are queued and ready
        readyLatch.await(5, TimeUnit.SECONDS);
        // Release all threads simultaneously
        startLatch.countDown();
        // Wait for all 100 operations to complete
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(true, finished, "All threads should complete within timeout");
        assertEquals(0, failureCount.get(), "Zero exceptions expected during concurrent updates");

        PostEntity post = jpaPostRepository.findById(testPostId).orElseThrow();
        assertEquals(100L, post.getUpvoteCount(), "Lost updates detected: expected 100 upvotes but got " + post.getUpvoteCount());
        assertEquals(100.0, post.getHotScore(), "hotScore must reflect 100 upvotes");
    }

    @Test
    @DisplayName("Concurrent downvotes after upvotes accurately calculate differential hotScore")
    void concurrentUpvotesAndDownvotes_accurateBalance() throws InterruptedException {
        int upvotes = 50;
        int downvotes = 20;
        int totalThreads = upvotes + downvotes;

        ExecutorService executor = Executors.newFixedThreadPool(32);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        for (int i = 0; i < upvotes; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    transactionTemplate.execute(status -> {
                        jpaPostRepository.updateUpvoteCountAtomic(testPostId, 1L);
                        return null;
                    });
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        for (int i = 0; i < downvotes; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    transactionTemplate.execute(status -> {
                        jpaPostRepository.updateDownvoteCountAtomic(testPostId, 1L);
                        return null;
                    });
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        PostEntity post = jpaPostRepository.findById(testPostId).orElseThrow();
        assertEquals(50L, post.getUpvoteCount());
        assertEquals(20L, post.getDownvoteCount());
        assertEquals(30.0, post.getHotScore(), "hotScore must be exactly 50 - 20 = 30.0");
    }

    @Test
    @DisplayName("Floor clamp prevents counters and hotScore from dropping below zero")
    void floorClamp_preventsNegativeValues() throws InterruptedException {
        int decrements = 30;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch doneLatch = new CountDownLatch(decrements);

        // Post initially has 0 upvotes. 30 concurrent threads decrement.
        for (int i = 0; i < decrements; i++) {
            executor.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        jpaPostRepository.updateUpvoteCountAtomic(testPostId, -1L);
                        return null;
                    });
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        PostEntity post = jpaPostRepository.findById(testPostId).orElseThrow();
        assertEquals(0L, post.getUpvoteCount(), "Upvote count should never drop below 0");
    }
}
