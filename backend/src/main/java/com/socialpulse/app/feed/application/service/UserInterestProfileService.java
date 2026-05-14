package com.socialpulse.app.feed.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class UserInterestProfileService {

    public Map<String, Double> buildKeywordProfile(Long userId) {
        return new HashMap<>();
    }

    public Map<String, Double> buildHashtagProfile(Long userId) {
        return new HashMap<>();
    }

    public double calculateKeywordRelevance(List<String> keywords, Map<String, Double> profile) {
        if (keywords.isEmpty() || profile.isEmpty()) {
            return 0.0;
        }

        double totalRelevance = 0.0;
        for (String keyword : keywords) {
            totalRelevance += profile.getOrDefault(keyword, 0.0);
        }

        return totalRelevance / keywords.size();
    }

    public double calculateHashtagRelevance(List<String> hashtags, Map<String, Double> profile) {
        if (hashtags.isEmpty() || profile.isEmpty()) {
            return 0.0;
        }

        double totalRelevance = 0.0;
        for (String hashtag : hashtags) {
            totalRelevance += profile.getOrDefault(hashtag, 0.0);
        }

        return totalRelevance / hashtags.size();
    }
}
