/**
 * Post-related utility functions
 */

import type { PulseReaction } from "@/services/post/postService";

/**
 * Calculate the next post pulse state when user clicks upvote
 * Handles toggling between upvote and no vote
 *
 * @param post - Post object with vote counts and current vote state
 * @returns Updated post object with new vote state
 *
 * @example
 * // User clicks upvote on a post they haven't voted on
 * const post = { myVote: null, upvoteCount: 10, downvoteCount: 2, myReaction: null };
 * const updated = nextPostPulseState(post);
 * // Result: { myVote: 1, upvoteCount: 11, downvoteCount: 2, myReaction: "UPVOTE" }
 *
 * @example
 * // User clicks upvote again to remove their vote
 * const post = { myVote: 1, upvoteCount: 11, downvoteCount: 2, myReaction: "UPVOTE" };
 * const updated = nextPostPulseState(post);
 * // Result: { myVote: 0, upvoteCount: 10, downvoteCount: 2, myReaction: null }
 */
export function nextPostPulseState<
  T extends {
    myVote: number | null;
    upvoteCount: number;
    downvoteCount: number;
    myReaction?: string | null;
  }
>(post: T): T {
  const currentVote = post.myVote ?? 0;
  const nextVote = currentVote === 1 ? 0 : 1;

  return {
    ...post,
    myVote: nextVote,
    myReaction: nextVote === 1 ? "UPVOTE" : null,
    upvoteCount: Math.max(
      0,
      post.upvoteCount + (nextVote === 1 ? 1 : 0) - (currentVote === 1 ? 1 : 0)
    ),
    downvoteCount: post.downvoteCount,
  };
}

/**
 * Get the numeric vote value from a reaction type
 * @param reaction - The reaction type (UPVOTE, DOWNVOTE, or null)
 * @returns 1 for upvote, -1 for downvote, 0 for no vote
 *
 * @example
 * getVoteValue("UPVOTE") // 1
 * getVoteValue("DOWNVOTE") // -1
 * getVoteValue(null) // 0
 */
export function getVoteValue(reaction: PulseReaction | null): number {
  if (reaction === "UPVOTE") return 1;
  if (reaction === "DOWNVOTE") return -1;
  return 0;
}
