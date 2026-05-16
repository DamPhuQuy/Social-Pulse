# Pushshift-only implicit-feedback recommender

This project uses the Pushshift Reddit dataset and therefore does not include exposure-level behavior logs such as impressions, clicks, dwell time, or feed ranking position. As a result, the system is not trained as a true CTR or exposure-aware ranking model. Instead, Reddit comments, replies, and submissions are treated as implicit positive feedback, while non-interacted sampled items are used as weak negatives. The project focuses on content-based and implicit-feedback recommendation.

## In scope

- content-based recommendation from Reddit text
- implicit-feedback recommendation from authored posts, comments, and replies
- retrieval-style ranking from user history
- chronological offline evaluation
- weak-negative sampling from non-interacted items

## Out of scope

- impression logging for training
- feature snapshots captured at impression time
- CTR prediction
- exposure-aware ranking
- dwell-time training labels
- true negative labels from shown-but-skipped items

## Training assumptions

Positive interactions are inferred from Pushshift records:

- user authored a submission
- user commented on a submission
- user replied in a thread
- user repeatedly participated in a subreddit

Weak negatives are sampled from items the user did not interact with. They are not true negatives and should be labeled `sampled_negative` or `weak_negative`.

## Leakage prevention

- split train, validation, and test chronologically
- build each user profile only from interactions before prediction time
- sample negatives from items available at or before the target timestamp
- do not use future aggregate statistics when building training or evaluation features

## Expected dataset shapes

Pairwise ranking:

```json
{
  "user_id": "...",
  "positive_item_id": "...",
  "negative_item_id": "...",
  "timestamp": "..."
}
```

Binary classification:

```json
{
  "user_id": "...",
  "item_id": "...",
  "label": 1,
  "timestamp": "...",
  "negative_type": null
}
```

```json
{
  "user_id": "...",
  "item_id": "...",
  "label": 0,
  "timestamp": "...",
  "negative_type": "sampled_negative"
}
```

Content retrieval:

```json
{
  "user_id": "...",
  "item_id": "...",
  "item_text": "title + selftext/body + subreddit",
  "timestamp": "..."
}
```

## Current helper module

`tools/pushshift_recommender/implicit_data.py` provides practical helpers for:

- building implicit positives from Pushshift-like records
- aggregating subreddit interactions
- weak-negative sampling with overlap protection
- pairwise sample generation
- chronological splitting
- leave-one-out holdout generation
