import { useEffect, useState } from "react";
import { Bookmark, Loader2 } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import { SafeAvatar } from "@/pages/ProfilePage";
import { deleteBookmark, getBookmarks } from "@/services/social/bookmarkService";
import type { UserPost } from "@/services/user/userService";

export default function BookmarksPage() {
  const navigate = useNavigate();
  const [posts, setPosts] = useState<UserPost[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadBookmarks();
  }, []);

  const loadBookmarks = async () => {
    setLoading(true);
    const res = await getBookmarks(0, 100);
    setLoading(false);
    if (res.ok && res.data) {
      setPosts(res.data.items ?? []);
    } else {
      toast.error(res.message ?? "Không thể tải bookmarks.");
    }
  };

  const removeBookmark = async (postId: number) => {
    const previous = posts;
    setPosts((prev) => prev.filter((item) => item.postId !== postId));
    const res = await deleteBookmark(postId);
    if (!res.ok) {
      setPosts(previous);
      toast.error(res.message ?? "Không thể xóa bookmark.");
    }
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-8 pt-24 px-6 lg:px-10">
        <AppSidebar active="bookmarks" />

        <div className="min-w-0">
          <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
          <div className="mb-4 flex items-center gap-2">
            <Bookmark className="h-5 w-5 text-blue-500" />
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">Bài viết đã lưu</h1>
          </div>
          {loading ? (
            <div className="flex justify-center py-16">
              <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
            </div>
          ) : posts.length === 0 ? (
            <div className="py-16 text-center text-slate-500 dark:text-neutral-500">Chưa có bài viết nào được lưu.</div>
          ) : (
            <div className="flex flex-col gap-4">
              {posts.map((post) => (
                <div key={post.postId} className="rounded-2xl border border-slate-200/80 p-5 dark:border-neutral-800">
                  <button onClick={() => navigate(`/posts/${post.postId}`)} className="w-full text-left">
                    <div className="mb-3 flex items-center gap-3">
                      <div className="h-10 w-10 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
                        <SafeAvatar src={post.userAvatar} alt={post.username} />
                      </div>
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-white">{post.username}</p>
                        <p className="text-xs text-slate-500 dark:text-neutral-400">{post.upvoteCount} pulse · {post.cmtCount} bình luận</p>
                      </div>
                    </div>
                    <p className="whitespace-pre-line text-sm leading-6 text-slate-700 dark:text-neutral-300">{post.content}</p>
                  </button>
                  <div className="mt-4">
                    <button onClick={() => removeBookmark(post.postId)} className="text-sm font-semibold text-red-600 hover:underline dark:text-red-400">
                      Bỏ lưu
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
          </section>
        </div>
      </div>
    </div>
  );
}
