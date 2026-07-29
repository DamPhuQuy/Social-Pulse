import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import {
  deletePost,
  reactPost,
  type FeedItem,
  type PulseReaction,
} from "@/features/feed/infrastructure/api/postService";
import {
  createBookmark,
  deleteBookmark,
} from "@/features/bookmarks/infrastructure/api/bookmarkService";
import { blockUser } from "@/features/social-relations/infrastructure/api/blockService";
import { nextPostPulseState } from "@/core/utils/postUtils";

export function usePostActions<T extends FeedItem>(
  posts: T[],
  setPosts: React.Dispatch<React.SetStateAction<T[]>>,
  bookmarkedPostIds: Set<number>,
  setBookmarkedPostIds: React.Dispatch<React.SetStateAction<Set<number>>>
) {
  const navigate = useNavigate();
  const { accessToken } = useAuth();
  const [reactingPostIds, setReactingPostIds] = useState<Set<number>>(() => new Set());
  const [bookmarkingPostIds, setBookmarkingPostIds] = useState<Set<number>>(() => new Set());
  const [editingPost, setEditingPost] = useState<T | null>(null);
  const [reportPostId, setReportPostId] = useState<number | null>(null);
  const [sharingPost, setSharingPost] = useState<T | null>(null);

  const handleReact = useCallback(async (postId: number, type: PulseReaction) => {
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập để bày tỏ cảm xúc.");
      navigate(PATHS.LOGIN);
      return;
    }
    if (reactingPostIds.has(postId)) return;

    const previousPost = posts.find((p) => p.postId === postId);
    if (!previousPost) return;

    setReactingPostIds((prev) => new Set(prev).add(postId));

    setPosts((prevFeed) =>
      prevFeed.map((post) => {
        if (post.postId === postId) {
          return nextPostPulseState(post);
        }
        return post;
      })
    );

    try {
      const res = await reactPost({ postId, reactionType: type });
      if (!res.ok) {
        setPosts((prevFeed) => prevFeed.map((p) => (p.postId === postId ? previousPost : p)));
        toast.error(res.message ?? "Thả cảm xúc thất bại.");
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
      toast.error("Vui lòng đăng nhập để bookmark bài viết.");
      navigate(PATHS.LOGIN);
      return;
    }
    if (bookmarkingPostIds.has(postId)) return;

    const wasBookmarked = bookmarkedPostIds.has(postId);
    setBookmarkingPostIds((prev) => new Set(prev).add(postId));
    setBookmarkedPostIds((prev) => {
      const next = new Set(prev);
      if (wasBookmarked) next.delete(postId);
      else next.add(postId);
      return next;
    });

    const res = wasBookmarked ? await deleteBookmark(postId) : await createBookmark(postId);
    if (!res.ok) {
      setBookmarkedPostIds((prev) => {
        const next = new Set(prev);
        if (wasBookmarked) next.add(postId);
        else next.delete(postId);
        return next;
      });
      toast.error(res.message ?? "Không thể cập nhật bookmark.");
    }

    setBookmarkingPostIds((prev) => {
      const next = new Set(prev);
      next.delete(postId);
      return next;
    });
  }, [accessToken, bookmarkingPostIds, bookmarkedPostIds, navigate, setBookmarkedPostIds]);

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

  const handleBlockUser = useCallback(async (userId: number, username: string) => {
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập để chặn người dùng.");
      navigate(PATHS.LOGIN);
      return;
    }
    if (!window.confirm(`Bạn có chắc chắn muốn chặn người dùng @${username}? Họ sẽ không thể xem bài viết của bạn.`)) {
      return;
    }
    const res = await blockUser(userId);
    if (res.ok) {
      toast.success(`Đã chặn @${username} thành công.`);
      setPosts((prev) => prev.filter((p) => p.userId !== userId));
    } else {
      toast.error(res.message ?? "Chặn người dùng thất bại.");
    }
  }, [accessToken, navigate, setPosts]);

  return {
    reactingPostIds,
    editingPost,
    setEditingPost,
    reportPostId,
    setReportPostId,
    sharingPost,
    setSharingPost,
    handleReact,
    handleToggleBookmark,
    handleDeletePost,
    handleBlockUser,
  };
}
