import { useState, useEffect, useCallback } from "react";
import { getFeed, getPostTopics, type FeedItem, type PostTopic } from "@/features/feed/infrastructure/api/postService";
import { getFollowedTopics, followTopic, unfollowTopic } from "@/features/discovery/infrastructure/api/topicService";

export type FeedMode = "discover" | "following" | "topic";

export function useFeed(initialTopicSlug?: string) {
  const [feed, setFeed] = useState<FeedItem[]>([]);
  const [feedLoading, setFeedLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [feedMode, setFeedMode] = useState<FeedMode>(initialTopicSlug ? "topic" : "discover");
  const [selectedTopic, setSelectedTopic] = useState<string | null>(initialTopicSlug || null);
  const [topics, setTopics] = useState<PostTopic[]>([]);
  const [followedTopicSlugs, setFollowedTopicSlugs] = useState<Set<string>>(() => new Set());

  const loadFeed = useCallback(async (mode: FeedMode = feedMode, topicSlug?: string) => {
    try {
      setFeedLoading(true);
      setPage(0);
      setHasMore(true);

      const targetTopic = mode === "topic" ? topicSlug : undefined;
      const res = await getFeed(0, 20, targetTopic);

      if (res.ok && res.data) {
        setFeed(res.data);
        if (res.data.length < 20) {
          setHasMore(false);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setFeedLoading(false);
    }
  }, [feedMode]);

  const loadMoreFeed = async () => {
    if (loadingMore || !hasMore) return;
    try {
      setLoadingMore(true);
      const nextPage = page + 1;
      const targetTopic = feedMode === "topic" ? (selectedTopic || undefined) : undefined;
      const res = await getFeed(nextPage, 20, targetTopic);

      if (res.ok && res.data) {
        if (res.data.length < 20) {
          setHasMore(false);
        }
        const existingIds = new Set(feed.map((item) => item.postId));
        const newItems = res.data.filter((item) => !existingIds.has(item.postId));
        setFeed((prev) => [...prev, ...newItems]);
        setPage(nextPage);
      } else {
        setHasMore(false);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingMore(false);
    }
  };

  const loadTopicsAndFollows = async () => {
    try {
      const [topicsRes, followedRes] = await Promise.all([
        getPostTopics(),
        getFollowedTopics().catch(() => ({ code: 500, message: "", data: [] as string[] }))
      ]);

      if (topicsRes.ok && topicsRes.data) {
        setTopics(topicsRes.data);
      }
      if (followedRes && followedRes.data) {
        setFollowedTopicSlugs(new Set(followedRes.data));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const toggleFollowTopic = async (slug: string) => {
    const isFollowing = followedTopicSlugs.has(slug);
    setFollowedTopicSlugs((prev) => {
      const next = new Set(prev);
      if (isFollowing) next.delete(slug);
      else next.add(slug);
      return next;
    });

    try {
      if (isFollowing) {
        await unfollowTopic(slug);
      } else {
        await followTopic(slug);
      }
    } catch (err) {
      console.error(err);
      setFollowedTopicSlugs((prev) => {
        const next = new Set(prev);
        if (isFollowing) next.add(slug);
        else next.delete(slug);
        return next;
      });
    }
  };

  useEffect(() => {
    loadTopicsAndFollows();
  }, []);

  return {
    feed,
    setFeed,
    feedLoading,
    loadingMore,
    hasMore,
    feedMode,
    setFeedMode,
    selectedTopic,
    setSelectedTopic,
    topics,
    followedTopicSlugs,
    loadFeed,
    loadMoreFeed,
    toggleFollowTopic,
  };
}
