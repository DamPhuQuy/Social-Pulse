import { useEffect, useState } from "react";
import { Bookmark, Loader2 } from "lucide-react";
import { toast } from "sonner";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { PostCard } from "@/features/feed/presentation/components/PostCard";
import { usePostActions } from "@/shared/hooks/usePostActions";
import { getBookmarks } from "@/features/bookmarks/infrastructure/api/bookmarkService";
import { getMyProfile } from "@/features/profiles/infrastructure/api/userService";
import type { FeedItem } from "@/features/feed/infrastructure/api/postService";
import CreatePostModal from "@/features/feed/presentation/components/CreatePostModal";
import ReportModal from "@/shared/components/ReportModal";

export default function BookmarksPage() {
  const [posts, setPosts] = useState<FeedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentUserId, setCurrentUserId] = useState<number | undefined>(undefined);
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(() => new Set());

  const {
    reactingPostIds,
    editingPost,
    setEditingPost,
    reportPostId,
    setReportPostId,
    setSharingPost,
    handleReact,
    handleToggleBookmark,
    handleDeletePost,
    handleBlockUser,
  } = usePostActions<FeedItem>(posts, setPosts, bookmarkedPostIds, setBookmarkedPostIds);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const profileRes = await getMyProfile().catch(() => ({ ok: false, data: undefined }));
      if (profileRes.ok && profileRes.data) setCurrentUserId(profileRes.data.userId);

      const res = await getBookmarks(0, 100);
      if (res.ok && res.data) {
        const items = res.data.items ?? [];
        const mapped: FeedItem[] = items.map((p: any) => ({
          postId: p.postId,
          parentPostId: p.parentPostId || null,
          type: p.type || "ORIGINAL",
          content: p.content || "",
          imageUrl: p.imageUrl || null,
          topicSlugs: p.topicSlugs || [],
          userId: p.userId,
          username: p.username || "",
          userAvatar: p.userAvatar || null,
          upvoteCount: p.upvoteCount || 0,
          downvoteCount: p.downvoteCount || 0,
          cmtCount: p.cmtCount || 0,
          shareCount: p.shareCount || 0,
          myReaction: p.myReaction || null,
          myVote: p.myVote || 0,
          rankingScore: null,
          source: null,
          rankingProvider: null,
          featureSchemaVersion: null,
          rankedAt: null,
          affinityScore: null,
          interactionCount30d: null,
          privacy: p.privacy || "PUBLIC",
          createdAt: p.createdAt,
          updatedAt: p.updatedAt || null,
          originalPost: p.originalPost || null,
        }));
        setPosts(mapped);
        setBookmarkedPostIds(new Set(mapped.map((p) => p.postId)));
      }
    } catch (e) {
      console.error(e);
      toast.error("Không thể tải bài viết đã lưu.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-[#ffffff] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-6 lg:gap-8 pt-6 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="bookmarks" />

        <div className="min-w-0 pb-24 lg:pb-10">
          <div className="mb-6 flex items-center gap-3 bg-white dark:bg-[#1e1e1e] p-6 rounded-3xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-sm">
            <Bookmark className="h-6 w-6 text-[#0064e0]" />
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Bài viết đã lưu</h1>
          </div>

          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 className="h-8 w-8 animate-spin text-[#0064e0]" />
            </div>
          ) : posts.length === 0 ? (
            <div className="py-16 text-center text-slate-400 font-semibold bg-white dark:bg-[#1e1e1e] rounded-3xl border border-slate-200/80 p-8">
              Chưa có bài viết nào được lưu.
            </div>
          ) : (
            <div className="flex flex-col gap-4">
              {posts.map((post) => (
                <PostCard
                  key={post.postId}
                  post={post}
                  currentUserId={currentUserId}
                  isBookmarked={true}
                  isReacting={reactingPostIds.has(post.postId)}
                  onReact={handleReact}
                  onToggleBookmark={handleToggleBookmark}
                  onEdit={setEditingPost}
                  onDelete={handleDeletePost}
                  onReport={setReportPostId}
                  onBlockUser={handleBlockUser}
                  onShare={setSharingPost}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      <CreatePostModal
        isOpen={!!editingPost}
        mode="edit"
        initialPost={editingPost}
        onClose={() => setEditingPost(null)}
        onPostUpdated={loadData}
      />
      <ReportModal
        isOpen={reportPostId !== null}
        targetType="POST"
        targetId={reportPostId}
        title="bài viết"
        onClose={() => setReportPostId(null)}
        onReportSuccess={loadData}
      />
      <BottomNavBar active="bookmarks" />
    </div>
  );
}
