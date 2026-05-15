package com.socialpulse.app.ai.lightgbm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class LightGbmModelScorerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void scoresTreeUsingNamedFeaturesAndDefaultLeftForExplicitMissing() throws Exception {
        String modelJson = """
                {
                  "objective": "lambdarank",
                  "feature_names": ["hot_score", "upvote_ratio"],
                  "tree_info": [
                    {
                      "shrinkage": 1.0,
                      "tree_structure": {
                        "split_feature": 0,
                        "threshold": 5.0,
                        "decision_type": "<=",
                        "default_left": true,
                        "left_child": { "leaf_value": -0.4 },
                        "right_child": {
                          "split_feature": 1,
                          "threshold": 0.8,
                          "decision_type": "<=",
                          "default_left": false,
                          "left_child": { "leaf_value": 0.2 },
                          "right_child": { "leaf_value": 0.9 }
                        }
                      }
                    }
                  ]
                }
                """;

        LightGbmModel model = objectMapper.readValue(modelJson, LightGbmModel.class);
        LightGbmModelScorer scorer = new LightGbmModelScorer(model);

        assertEquals(-0.4, scorer.score(Map.of("hot_score", 2.0)), 1e-9);
        assertEquals(0.2, scorer.score(Map.of("hot_score", 6.0, "upvote_ratio", 0.5)), 1e-9);
        assertEquals(0.9, scorer.score(Map.of("hot_score", 6.0, "upvote_ratio", 0.9)), 1e-9);
        assertEquals(-0.4, scorer.score(Map.of("hot_score", Double.NaN, "upvote_ratio", 0.9)), 1e-9);
    }

    @Test
    void usesZeroDefaultForMissingFeatureKeysAndSupportsStrictComparisons() throws Exception {
        String modelJson = """
                {
                  "objective": "lambdarank",
                  "feature_names": ["affinity_score", "hot_score"],
                  "tree_info": [
                    {
                      "shrinkage": 1.0,
                      "tree_structure": {
                        "split_feature": 0,
                        "threshold": 0.5,
                        "decision_type": "<",
                        "default_left": false,
                        "left_child": { "leaf_value": 0.1 },
                        "right_child": {
                          "split_feature": 1,
                          "threshold": 10.0,
                          "decision_type": "==",
                          "default_left": true,
                          "left_child": { "leaf_value": 0.4 },
                          "right_child": { "leaf_value": 0.9 }
                        }
                      }
                    }
                  ]
                }
                """;

        LightGbmModel model = objectMapper.readValue(modelJson, LightGbmModel.class);
        LightGbmModelScorer scorer = new LightGbmModelScorer(model);

        assertEquals(0.1, scorer.score(Map.of()), 1e-9);
        assertEquals(0.4, scorer.score(Map.of("affinity_score", 0.5, "hot_score", 10.0)), 1e-9);
        assertEquals(0.9, scorer.score(Map.of("affinity_score", 0.5, "hot_score", 8.0)), 1e-9);
    }
}
