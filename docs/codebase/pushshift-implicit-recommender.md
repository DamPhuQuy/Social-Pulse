# Pushshift Implicit Recommender

Social Pulse uses Pushshift Reddit data as an offline proxy for feed ranking.
The dataset does not contain real Social Pulse impressions, clicks, dwell time,
or feed positions, so the trained model is a cold-start ranking model rather
than a full production recommender.

## Label

The label is derived from final Reddit engagement:

```text
label = log1p(max(score, 0) + max(num_comments, 0) + max(num_crossposts, 0))
```

These fields are target labels only. They are not model inputs in schema v2.

## Personalization Proxy

Comments are used to infer prior viewer-author affinity:

- count of prior interactions in 7 days
- count of prior interactions in 30 days
- hours since latest prior interaction
- interaction share relative to the viewer's total activity

Only interactions before the current post are used.

## Limitations

- No true impression negatives.
- No dwell time or click-through labels.
- Reddit behavior may differ from Social Pulse behavior.
- Final validation must be followed by real application monitoring after deploy.

## Why It Is Still Useful

The model can learn cold-start priors from content structure, author history,
post age, and prior affinity. Once Social Pulse has enough real behavior logs,
the same schema can be extended or replaced with first-party training data.
