import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  MessageCircle,
  Share2,
  Bookmark,
  MoreHorizontal,
  Edit3,
  Trash2,
  UserX,
  ShieldCheck,
  Activity,
  Flame
} from "lucide-react";
import { toast } from "sonner";
import { PATHS } from "@/shared/constants/paths";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { PostMedia } from "@/features/feed/presentation/components/PostMedia";
import CommentSection from "@/features/feed/presentation/components/CommentSection";
import { timeAgo } from "@/core/utils/dateUtils";
import type { FeedItem, PulseReaction } from "@/features/feed/infrastructure/api/postService";

export interface PostCardProps {
  post: FeedItem;
  rank?: number;
  currentUserId?: number;
  isBookmarked: boolean;
  isReacting?: boolean;
  onReact: (postId: number, type: PulseReaction) => void;
  onToggleBookmark: (postId: number) => void;
  onEdit: (post: FeedItem) => void;
  onDelete: (postId: number) => void;
  onReport: (postId: number) => void;
  onBlockUser?: (userId: number, username: string) => void;
  onShare?: (post: FeedItem) => void;
}

export const PostCard: React.FC<PostCardProps> = ({
  post,
  rank,
  currentUserId,
  isBookmarked,
  isReacting,
  onReact,
  onToggleBookmark,
  onEdit,
  onDelete,
  onReport,
  onBlockUser,
  onShare,
}) => {
  const navigate = useNavigate();
  const isAuthor = currentUserId === post.userId;
  const isUpvoted = post.myVote === 1;
  const [showComments, setShowComments] = useState(false);
  const [cmtCount, setCmtCount] = useState(post.cmtCount);
  const [showMenu, setShowMenu] = useState(false);

  useEffect(() => {
    setCmtCount(post.cmtCount);
  }, [post.cmtCount]);

  const navigateToProfile = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!currentUserId) {
      toast.error("Vui lòng đăng nhập để xem thông tin người dùng.");
      navigate(PATHS.LOGIN);
      return;
    }
    navigate(`/profile/${post.username}`);
  };

  return (
    <article className="bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] rounded-3xl p-6 shadow-[0_4px_20px_rgba(0,0,0,0.03)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.4)] hover:border-slate-300 dark:hover:border-neutral-700 transition-all duration-300">
      <div className="flex gap-4">
        {/* Avatar */}
        <div
          onClick={navigateToProfile}
          className="shrink-0 w-11 h-11 rounded-full overflow-hidden bg-slate-100 dark:bg-neutral-800 cursor-pointer hover:opacity-85 transition-opacity"
        >
          <SafeAvatar src={post.userAvatar} alt={post.username} />
        </div>

        <div className="flex-1 min-w-0">
          {/* Header Bar */}
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2 flex-wrap">
              <span
                onClick={navigateToProfile}
                className="font-bold text-slate-900 dark:text-[#e4e6eb] truncate cursor-pointer hover:underline text-base"
              >
                {post.username}
              </span>
              {rank !== undefined && (
                <span className="inline-flex items-center rounded-full bg-slate-900 text-white dark:bg-white dark:text-slate-900 px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wide">
                  Rank #{rank}
                </span>
              )}
              {post.source === "POPULAR" && (
                <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 px-2.5 py-0.5 text-[10px] font-bold">
                  <Flame className="w-3 h-3" /> Nổi bật
                </span>
              )}
              <span className="text-xs text-slate-400 dark:text-neutral-500 font-medium">
                • {timeAgo(post.createdAt)}
              </span>
            </div>

            {/* Menu */}
            <div className="relative">
              <button
                onClick={() => setShowMenu((prev) => !prev)}
                className="p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-800 text-slate-400 hover:text-slate-600 dark:hover:text-neutral-200 transition-colors"
              >
                <MoreHorizontal className="w-5 h-5" />
              </button>
              {showMenu && (
                <div
                  className="absolute right-0 mt-2 w-48 bg-white dark:bg-[#252525] border border-slate-200 dark:border-neutral-700 rounded-2xl shadow-xl z-50 py-1.5 animate-in fade-in zoom-in-95 duration-150"
                  onMouseLeave={() => setShowMenu(false)}
                >
                  {isAuthor ? (
                    <>
                      <button
                        onClick={() => {
                          setShowMenu(false);
                          onEdit(post);
                        }}
                        className="w-full text-left px-4 py-2 text-sm font-semibold text-slate-700 dark:text-neutral-200 hover:bg-slate-50 dark:hover:bg-neutral-700/50 flex items-center gap-2"
                      >
                        <Edit3 className="w-4 h-4" /> Chỉnh sửa
                      </button>
                      <button
                        onClick={() => {
                          setShowMenu(false);
                          onDelete(post.postId);
                        }}
                        className="w-full text-left px-4 py-2 text-sm font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 flex items-center gap-2"
                      >
                        <Trash2 className="w-4 h-4" /> Xóa bài viết
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() => {
                          setShowMenu(false);
                          onReport(post.postId);
                        }}
                        className="w-full text-left px-4 py-2 text-sm font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 flex items-center gap-2"
                      >
                        <ShieldCheck className="w-4 h-4" /> Báo cáo
                      </button>
                      {onBlockUser && (
                        <button
                          onClick={() => {
                            setShowMenu(false);
                            onBlockUser(post.userId, post.username);
                          }}
                          className="w-full text-left px-4 py-2 text-sm font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 flex items-center gap-2"
                        >
                          <UserX className="w-4 h-4" /> Chặn @{post.username}
                        </button>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Topic Tags */}
          {post.topicSlugs && post.topicSlugs.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mb-3">
              {post.topicSlugs.map((slug) => (
                <span
                  key={slug}
                  onClick={() => navigate(`${PATHS.HOME}?topic=${slug}`)}
                  className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-slate-100 text-slate-700 dark:bg-neutral-800 dark:text-neutral-300 hover:bg-blue-50 hover:text-blue-600 cursor-pointer transition-colors"
                >
                  #{slug}
                </span>
              ))}
            </div>
          )}

          {/* Content */}
          <p className="text-slate-800 dark:text-[#e4e6eb] text-sm sm:text-base leading-relaxed whitespace-pre-wrap mb-4">
            {post.content}
          </p>

          {/* Media */}
          {post.imageUrl && (
            <div className="mb-4 rounded-2xl overflow-hidden border border-slate-100 dark:border-neutral-800">
              <PostMedia mediaUrl={post.imageUrl} />
            </div>
          )}

          {/* Action Bar */}
          <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-[#2a2a2a]">
            {/* Reaction Button */}
            <button
              onClick={() => onReact(post.postId, isUpvoted ? "NONE" : "UPVOTE")}
              disabled={isReacting}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs sm:text-sm font-bold transition-all ${
                isUpvoted
                  ? "bg-blue-50 text-blue-600 dark:bg-blue-950/40 dark:text-blue-400"
                  : "text-slate-500 dark:text-neutral-400 hover:bg-slate-100 dark:hover:bg-neutral-800"
              }`}
            >
              <Activity className={`w-4 h-4 ${isUpvoted ? "stroke-[2.5px]" : ""}`} />
              <span>{post.upvoteCount}</span>
            </button>

            {/* Comment Button */}
            <button
              onClick={() => setShowComments((prev) => !prev)}
              className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs sm:text-sm font-semibold text-slate-500 dark:text-neutral-400 hover:bg-slate-100 dark:hover:bg-neutral-800 transition-colors"
            >
              <MessageCircle className="w-4 h-4" />
              <span>{cmtCount}</span>
            </button>

            {/* Bookmark Button */}
            <button
              onClick={() => onToggleBookmark(post.postId)}
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs sm:text-sm font-semibold transition-colors ${
                isBookmarked
                  ? "text-blue-600 dark:text-blue-400 font-bold"
                  : "text-slate-500 dark:text-neutral-400 hover:bg-slate-100 dark:hover:bg-neutral-800"
              }`}
            >
              <Bookmark className={`w-4 h-4 ${isBookmarked ? "fill-blue-600 text-blue-600 dark:fill-blue-400 dark:text-blue-400" : ""}`} />
            </button>

            {/* Share Button */}
            {onShare && (
              <button
                onClick={() => onShare(post)}
                className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs sm:text-sm font-semibold text-slate-500 dark:text-neutral-400 hover:bg-slate-100 dark:hover:bg-neutral-800 transition-colors"
              >
                <Share2 className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Comment Section Expandable */}
          {showComments && (
            <div className="mt-4 pt-4 border-t border-slate-100 dark:border-[#2a2a2a]">
              <CommentSection
                postId={post.postId}
                initialCmtCount={cmtCount}
                onCommentCountChange={(count) => setCmtCount(count)}
              />
            </div>
          )}
        </div>
      </div>
    </article>
  );
};
