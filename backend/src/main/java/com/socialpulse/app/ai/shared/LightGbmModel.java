package com.socialpulse.app.ai.shared;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightGbmModel {
    @JsonProperty("feature_names")
    private List<String> featureNames = List.of();

    @JsonProperty("tree_info")
    private List<TreeInfo> treeInfo = List.of();

    @JsonProperty("average_output")
    private Boolean averageOutput = false;
    private JsonNode objective;

    @JsonIgnore
    public String getObjectiveName() {
        if (objective == null || objective.isNull()) {
            return "";
        }

        if (objective.isTextual()) {
            return objective.asText("");
        }

        JsonNode name = objective.get("name");
        return name != null ? name.asText("") : "";
    }

    @JsonIgnore
    public String getFeatureName(int featureIndex) {
        if (featureIndex < 0 || featureIndex >= featureNames.size()) {
            throw new IllegalArgumentException("Feature index out of bounds: " + featureIndex);
        }
        return featureNames.get(featureIndex);
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TreeInfo {
        private Double shrinkage = 1.0;

        @JsonProperty("tree_structure")
        private TreeNode treeStructure;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TreeNode {
        @JsonProperty("split_feature")
        private Integer splitFeature;

        private Double threshold;

        @JsonProperty("decision_type")
        private String decisionType;

        @JsonProperty("default_left")
        private Boolean defaultLeft;

        @JsonProperty("missing_type")
        private String missingType;

        @JsonProperty("left_child")
        private TreeNode leftChild;

        @JsonProperty("right_child")
        private TreeNode rightChild;

        @JsonProperty("leaf_value")
        private Double leafValue;

        @JsonIgnore
        public boolean isLeaf() {
            return leafValue != null || (leftChild == null && rightChild == null);
        }
    }
}
