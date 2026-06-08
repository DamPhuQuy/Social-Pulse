package com.socialpulse.app.common.schedule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.post.domain.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SyncSchedule {

    private static final String SHARE_KEYS_SET = "share:delta:keys";

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;

    public SyncSchedule(StringRedisTemplate redisTemplate, PostRepository postRepository) {
        this.redisTemplate = redisTemplate;
        this.postRepository = postRepository;
    }

    @Scheduled(fixedRate = 10000)
    @Transactional()
    public void syncShareCount() {
        Set<String> keys = redisTemplate.opsForSet().members(SHARE_KEYS_SET);

        if (keys == null || keys.isEmpty()) {
            return;
        }

        // using map to avoid open too many transactions when updating database
        Map<Long, Long> updates = new HashMap<>();

        for (String key : keys) {
            // Set value of key and return its old value.
            String deltaStr = redisTemplate.opsForValue().getAndSet(key, "0");

            if (deltaStr == null || deltaStr.equals("0")) {
                redisTemplate.opsForSet().remove(SHARE_KEYS_SET, key);
                continue;
            }

            try {
                Long postId = Long.parseLong(key.split(":")[1]);
                Long delta = Long.parseLong(deltaStr);
                updates.put(postId, delta);
            } catch(Exception e) {
                log.error("Failed to parse share count delta for key: " + key, e);
            }
        }

        if (updates.isEmpty()) return;

        try {
            postRepository.updateShareCount(updates);

            updates.keySet().forEach(id -> {
                String redisKey = "post:" + id + ":shareCount:delta";
                redisTemplate.opsForSet().remove(SHARE_KEYS_SET, redisKey);
            });
        } catch (Exception e) {
            log.error("Failed to update share count for updates: " + updates, e);

            // rollback
            updates.forEach((id, delta) -> {
                String redisKey = "post:" + id + ":shareCount:delta";
                redisTemplate.opsForValue().increment(redisKey, delta);
            });
        }
    }
}
