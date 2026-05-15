package com.socialpulse.app.ai.training;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PushshiftTrainingCli {
    private PushshiftTrainingCli() {
    }


    public static void main(String[] args) throws Exception {
        TrainingArguments arguments = TrainingArguments.parse(args);
        TrainingRunResult result = new PushshiftTrainingPipeline().run(arguments);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("output", result.outputPath().toString());
        output.put("trained_at", result.trainedAt());
        output.put("train_rmse", TrainingJsonSupport.round(result.metrics().trainRmse()));
        output.put("validation_rmse", TrainingJsonSupport.round(result.metrics().validationRmse()));

        Map<String, Object> rows = new LinkedHashMap<>();
        rows.put("train", result.trainRows());
        rows.put("validation", result.validationRows());
        output.put("rows", rows);

    System.out.println(TrainingJsonSupport.toPrettyJson(output));
    }
}
