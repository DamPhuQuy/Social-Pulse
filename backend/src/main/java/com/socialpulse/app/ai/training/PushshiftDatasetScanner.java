package com.socialpulse.app.ai.training;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

final class PushshiftDatasetScanner {
    private static final double HOT_SCORE_TIME_DIVISOR = 45000.0;
    private static final long REDDIT_EPOCH = 1134028003L;

    ScanResult scanSubmissions(TrainingArguments arguments) throws IOException {
        Random random = new Random(arguments.seed());
        List<SubmissionRecord> reservoir = new ArrayList<>(arguments.sampleSize());
        Map<String, AuthorAggregate> authorAggregates = new HashMap<>();

        int scanned = 0;
        int filtered = 0;
        int accepted = 0;

        try (TrainingJsonSupport.JsonLineReader reader = new TrainingJsonSupport.JsonLineReader(arguments.submissionsPath())) {
            JsonNode payload;
            while ((payload = reader.readNext()) != null) {
                scanned++;

                SubmissionRecord record = preprocessSubmission(payload, arguments.minContentLength());
                if (record == null) {
                    filtered++;
                    continue;
                }

                accepted++;
                double popularity = popularity(record.score(), record.numComments(), record.numCrossposts());
                AuthorAggregate aggregate = authorAggregates.computeIfAbsent(record.author(), ignored -> new AuthorAggregate());
                aggregate.increment(popularity);

                if (reservoir.size() < arguments.sampleSize()) {
                    reservoir.add(record);
                } else {
                    int replacementIndex = random.nextInt(accepted);
                    if (replacementIndex < arguments.sampleSize()) {
                        reservoir.set(replacementIndex, record);
                    }
                }

                if (accepted >= arguments.scanLimitPosts()) {
                    break;
                }
            }
        }

        Map<String, Integer> scanStats = new LinkedHashMap<>();
        scanStats.put("submissions_scanned", scanned);
        scanStats.put("submissions_filtered", filtered);
        scanStats.put("submissions_accepted", accepted);
        scanStats.put("reservoir_size", reservoir.size());
        return new ScanResult(List.copyOf(reservoir), Map.copyOf(authorAggregates), scanStats);
    }

    Map<String, Integer> scanComments(Path commentsPath, List<SubmissionRecord> sampledPosts, int scanLimitComments)
            throws IOException {
        Set<String> sampledPostIds = new HashSet<>();
        for (SubmissionRecord sampledPost : sampledPosts) {
            sampledPostIds.add(sampledPost.postId());
        }

        int scanned = 0;
        int matched = 0;

        try (TrainingJsonSupport.JsonLineReader reader = new TrainingJsonSupport.JsonLineReader(commentsPath)) {
            JsonNode payload;
            while ((payload = reader.readNext()) != null) {
                scanned++;

                String linkId = TrainingJsonSupport.textValue(payload.get("link_id"));
                String postId = TrainingJsonSupport.stripThingPrefix(linkId);
                if (!postId.isBlank() && sampledPostIds.contains(postId)) {
                    matched++;
                }

                if (scanned >= scanLimitComments) {
                    break;
                }
            }
        }

        Map<String, Integer> commentStats = new LinkedHashMap<>();
        commentStats.put("comments_scanned", scanned);
        commentStats.put("matched_sample_posts", matched);
        return commentStats;
    }

    private SubmissionRecord preprocessSubmission(JsonNode payload, int minContentLength) {
        String author = TrainingJsonSupport.normalizeText(TrainingJsonSupport.textValue(payload.get("author")));
        String title = TrainingJsonSupport.normalizeText(TrainingJsonSupport.textValue(payload.get("title")));
        String body = TrainingJsonSupport.normalizeText(TrainingJsonSupport.textValue(payload.get("selftext")));
        double createdUtc = TrainingJsonSupport.doubleValue(payload.get("created_utc"));
        double retrievedOn = payload.hasNonNull("retrieved_on")
                ? TrainingJsonSupport.doubleValue(payload.get("retrieved_on"))
                : createdUtc;
        int score = Math.max(0, TrainingJsonSupport.intValue(payload.get("score")));
        int numComments = Math.max(0, TrainingJsonSupport.intValue(payload.get("num_comments")));
        int numCrossposts = Math.max(0, TrainingJsonSupport.intValue(payload.get("num_crossposts")));

        if (author.isBlank() || "[deleted]".equalsIgnoreCase(author) || "automoderator".equalsIgnoreCase(author)) {
            return null;
        }
        if (createdUtc <= 0) {
            return null;
        }
        if (title.isBlank() && body.isBlank()) {
            return null;
        }

        int titleLength = title.length();
        int bodyLength = body.length();
        if (titleLength + bodyLength < minContentLength) {
            return null;
        }

        String postId = TrainingJsonSupport.normalizeText(TrainingJsonSupport.textValue(payload.get("id")));
        if (postId.isBlank()) {
            return null;
        }

        return new SubmissionRecord(
                postId,
                author,
                TrainingJsonSupport.optionalDoubleValue(payload.get("author_created_utc")),
                createdUtc,
                retrievedOn,
                titleLength,
                bodyLength,
                score,
                numComments,
                numCrossposts,
                detectMultimedia(payload),
                detectSharePost(payload),
                redditHotScore(score, createdUtc));
    }

    static double popularity(int score, int numComments, int numCrossposts) {
        return Math.max(score, 0) + numComments + numCrossposts;
    }

    private boolean detectMultimedia(JsonNode payload) {
        if (payload.path("is_video").asBoolean(false)) {
            return true;
        }
        if (!payload.path("media").isMissingNode() && !payload.path("media").isNull()) {
            return true;
        }
        if (!payload.path("secure_media").isMissingNode() && !payload.path("secure_media").isNull()) {
            return true;
        }

        String thumbnail = TrainingJsonSupport.normalizeText(TrainingJsonSupport.textValue(payload.get("thumbnail")));
        if (!thumbnail.isBlank()
                && !"self".equalsIgnoreCase(thumbnail)
                && !"default".equalsIgnoreCase(thumbnail)
                && !"nsfw".equalsIgnoreCase(thumbnail)
                && !"image".equalsIgnoreCase(thumbnail)) {
            return true;
        }

        String url = TrainingJsonSupport.textValue(payload.get("url")).toLowerCase(Locale.ROOT);
        return url.endsWith(".jpg")
                || url.endsWith(".jpeg")
                || url.endsWith(".png")
                || url.endsWith(".gif")
                || url.endsWith(".webp")
                || url.endsWith(".mp4")
                || url.endsWith(".mov");
    }

    private boolean detectSharePost(JsonNode payload) {
        return TrainingJsonSupport.intValue(payload.get("num_crossposts")) > 0 || payload.hasNonNull("crosspost_parent");
    }

    private double redditHotScore(int score, double createdUtc) {
        double order = Math.log10(Math.max(Math.abs(score), 1));
        double sign = score > 0 ? 1.0 : score < 0 ? -1.0 : 0.0;
        double seconds = createdUtc - REDDIT_EPOCH;
        return TrainingJsonSupport.round(sign * order + seconds / HOT_SCORE_TIME_DIVISOR);
    }
}
