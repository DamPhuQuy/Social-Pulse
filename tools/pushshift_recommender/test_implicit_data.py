import unittest

from .implicit_data import (
    build_binary_samples,
    build_item_text,
    build_leave_one_out_holdout,
    build_pairwise_samples,
    build_positive_interactions,
    build_subreddit_interactions,
    chronological_split,
)


class ImplicitDataTest(unittest.TestCase):
    def setUp(self):
        self.records = [
            {"author": "u1", "id": "p1", "subreddit": "python", "title": "Post 1", "selftext": "hello", "created_utc": 100},
            {"author": "u1", "id": "p1", "subreddit": "python", "title": "Post 1 duplicate", "selftext": "hello again", "created_utc": 110},
            {"author": "u1", "id": "c1", "post_id": "p2", "subreddit": "python", "body": "comment body", "parent_id": "t3_p2", "created_utc": 120},
            {"author": "u2", "id": "c2", "post_id": "p3", "subreddit": "java", "body": "reply body", "parent_id": "t1_c1", "created_utc": 130},
            {"author": "u2", "id": "p4", "subreddit": "java", "title": "Post 4", "selftext": "body4", "created_utc": 140},
        ]
        self.catalog = [
            {"id": "p1", "subreddit": "python", "created_utc": 100},
            {"id": "p2", "subreddit": "python", "created_utc": 110},
            {"id": "p3", "subreddit": "java", "created_utc": 120},
            {"id": "p4", "subreddit": "java", "created_utc": 130},
            {"id": "p5", "subreddit": "python", "created_utc": 125},
        ]

    def test_build_positive_interactions_deduplicates_user_item(self):
        interactions = build_positive_interactions(self.records)
        keys = [(item.user_id, item.item_id) for item in interactions]
        self.assertEqual(len(interactions), len(set(keys)))
        self.assertEqual([item.event_type for item in interactions], ["post", "comment", "reply", "post"])

    def test_build_subreddit_interactions_counts_participation(self):
        interactions = build_positive_interactions(self.records)
        subreddit_rows = build_subreddit_interactions(interactions)
        self.assertEqual(subreddit_rows[0]["interaction_count"], 2)
        self.assertEqual(subreddit_rows[0]["subreddit"], "python")

    def test_negative_sampling_avoids_positive_overlap_and_deduplicates(self):
        interactions = build_positive_interactions(self.records)
        samples = build_binary_samples(interactions, self.catalog, negatives_per_positive=2, seed=3)
        positives = {(sample.user_id, sample.item_id) for sample in samples if sample.label == 1}
        negatives = [(sample.user_id, sample.item_id) for sample in samples if sample.label == 0]
        self.assertEqual(len(negatives), len(set(negatives)))
        self.assertTrue(all(negative not in positives for negative in negatives))

    def test_pairwise_samples_use_sampled_negatives(self):
        interactions = build_positive_interactions(self.records)
        samples = build_binary_samples(interactions, self.catalog, negatives_per_positive=1, seed=5)
        pairwise = build_pairwise_samples(samples)
        self.assertTrue(len(pairwise) > 0)
        self.assertTrue(all(item.positive_item_id != item.negative_item_id for item in pairwise))

    def test_chronological_split_is_time_ordered(self):
        interactions = build_positive_interactions(self.records)
        split = chronological_split(interactions, train_ratio=0.5, validation_ratio=0.25)
        merged = split["train"] + split["validation"] + split["test"]
        self.assertEqual([item.timestamp for item in merged], sorted(item.timestamp for item in merged))

    def test_leave_one_out_uses_only_past_history(self):
        interactions = build_positive_interactions(self.records)
        holdouts = build_leave_one_out_holdout(interactions)
        self.assertEqual(len(holdouts), 2)
        self.assertTrue(all(example.history[-1].timestamp < example.target.timestamp for example in holdouts))

    def test_build_item_text_omits_removed_values(self):
        text = build_item_text({"title": "Hello", "selftext": "[removed]", "body": "", "subreddit": "python"})
        self.assertEqual(text, "Hello python")


if __name__ == "__main__":
    unittest.main()
