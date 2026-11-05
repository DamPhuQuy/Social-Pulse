import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { useAuth } from "@/shared/hooks/useAuth";
import {
  deletePost,
  reactPost,
  type FeedItem,
  type PulseReaction,
} from "@/features/feed/infrastructure/api/postService";
import { nextPostPulseState } from "@/core/utils/postUtils";

import { toggleBookmark as apiToggleBookmark } from "@/features/bookmarks/infrastructure/api/bookmarkService";

export function usePostActions<T extends FeedItem>(
  posts: T[],
  setPosts: React.Dispatch<React.SetStateAction<T[]>>
) {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [reactingPostIds, setReactingPostIds] = useState<Set<number>>(() => new Set());
  const [editingPost, setEditingPost] = useState<T | null>(null);
  const [sharingPost, setSharingPost] = useState<T | null>(null);

  const handleReact = useCallback(async (postId: number, type: PulseReaction) => {
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập để upvote bài viết.");
      return;
    }
    if (reactingPostIds.has(postId)) return;

    setReactingPostIds((prev) => new Set(prev).add(postId));

    const currentPost = posts.find((p) => p.postId === postId);
    if (!currentPost) {
      setReactingPostIds((prev) => {
        const next = new Set(prev);
        next.delete(postId);
        return next;
      });
      return;
    }

    const previousPost = { ...currentPost };
    const nextState = nextPostPulseState(currentPost);

    setPosts((prevFeed) =>
      prevFeed.map((p) => (p.postId === postId ? nextState : p))
    );

    try {
      const res = await reactPost({ postId, reactionType: type });
      if (!res.ok) {
        setPosts((prevFeed) => prevFeed.map((p) => (p.postId === postId ? previousPost : p)));
        toast.error(res.message ?? "Lỗi khi cập nhật upvote.");
      }
    } catch (err) {
      console.error(err);
      setPosts((prevFeed) => prevFeed.map((p) => (p.postId === postId ? previousPost : p)));
    } finally {
      setReactingPostIds((prev) => {
        const next = new Set(prev);
        next.delete(postId);
        return next;
      });
    }
  }, [accessToken, navigate, posts, reactingPostIds, setPosts]);

  const handleToggleBookmark = useCallback(async (postId: number) => {
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập để lưu bài viết.");
      return;
    }
    const currentPost = posts.find((p) => p.postId === postId);
    if (!currentPost) return;

    const nextBookmarked = !currentPost.isBookmarked;
    setPosts((prevFeed) =>
      prevFeed.map((p) => (p.postId === postId ? { ...p, isBookmarked: nextBookmarked } : p))
    );

    try {
      const res = await apiToggleBookmark(postId);
      if (!res.ok) {
        setPosts((prevFeed) =>
          prevFeed.map((p) => (p.postId === postId ? { ...p, isBookmarked: currentPost.isBookmarked } : p))
        );
        toast.error(res.message ?? "Không thể lưu bài viết.");
      } else {
        toast.success(res.bookmarked ? "Đã lưu vào danh sách đọc 🔖" : "Đã bỏ lưu bài viết");
      }
    } catch {
      setPosts((prevFeed) =>
        prevFeed.map((p) => (p.postId === postId ? { ...p, isBookmarked: currentPost.isBookmarked } : p))
      );
    }
  }, [accessToken, posts, setPosts]);

  const handleDeletePost = useCallback(async (postId: number) => {
    const previousPosts = posts;
    setPosts((prevFeed) => prevFeed.filter((p) => p.postId !== postId));

    const result = await deletePost(postId);
    if (!result.ok) {
      setPosts(previousPosts);
      toast.error(result.message ?? "Xóa bài viết thất bại.");
      return;
    }
    toast.success("Đã xóa bài viết.");
  }, [posts, setPosts]);

  return {
    reactingPostIds,
    editingPost,
    setEditingPost,
    sharingPost,
    setSharingPost,
    handleReact,
    handleToggleBookmark,
    handleDeletePost,
  };
}
