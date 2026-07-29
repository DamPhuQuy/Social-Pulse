/**
 * Post-related utility functions
 */

import type { PulseReaction } from "@/features/feed/infrastructure/api/postService";

/**
 * Calculate the next post pulse state when user clicks upvote
 * Handles toggling between upvote and no vote
 *
 * @param post - Post object with vote counts and current vote state
 * @returns Updated post object with new vote state
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
 * @param reaction - The reaction type (UPVOTE, NONE, or null)
 * @returns 1 for upvote, 0 for no vote
 */
export function getVoteValue(reaction: PulseReaction | null): number {
  if (reaction === "UPVOTE") return 1;
  return 0;
}
