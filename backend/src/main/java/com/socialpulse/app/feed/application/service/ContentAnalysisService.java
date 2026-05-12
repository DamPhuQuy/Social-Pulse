package com.socialpulse.app.feed.application.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class ContentAnalysisService {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#\\w+");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");

    public List<String> extractKeywords(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        String cleaned = content.replaceAll("#\\w+", "")
                               .replaceAll("@\\w+", "")
                               .replaceAll("https?://\\S+", "")
                               .toLowerCase();

        return Arrays.stream(cleaned.split("\\s+"))
                    .filter(word -> word.length() > 3)
                    .filter(word -> word.matches("[a-z]+"))
                    .distinct()
                    .collect(Collectors.toList());
    }

    public List<String> extractHashtags(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> hashtags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            hashtags.add(matcher.group().toLowerCase());
        }
        return hashtags;
    }

    public List<String> extractMentions(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group().toLowerCase());
        }
        return mentions;
    }

    public int getContentLength(String content) {
        return content == null ? 0 : content.length();
    }

    public boolean containsHashtags(String content) {
        return content != null && HASHTAG_PATTERN.matcher(content).find();
    }

    public boolean containsUrl(String content) {
        return content != null && URL_PATTERN.matcher(content).find();
    }

    public boolean containsMentions(String content) {
        return content != null && MENTION_PATTERN.matcher(content).find();
    }
}
