package com.socialpulse.app.ai.lightgbm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LightGbmModelArtifact {
    @JsonProperty("artifact_version")
    private String artifactVersion = "1";

    @JsonProperty("feature_schema_version")
    private String featureSchemaVersion;

    @JsonProperty("training_dataset")
    private String trainingDataset;

    @JsonProperty("trained_at")
    private String trainedAt;

    @JsonProperty("label_strategy")
    private String labelStrategy;

    @JsonProperty("model_dump")
    private LightGbmModel modelDump;
}
