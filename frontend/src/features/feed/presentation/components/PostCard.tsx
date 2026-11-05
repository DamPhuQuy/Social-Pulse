import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  MessageCircle,
  Share2,
  MoreHorizontal,
  Edit3,
  Trash2,
  Activity,
  Flame
} from "lucide-react";
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
  isReacting?: boolean;
  onReact: (postId: number, type: PulseReaction) => void;
  onEdit: (post: FeedItem) => void;
  onDelete: (postId: number) => void;
  onShare?: (post: FeedItem) => void;
}

export const PostCard: React.FC<PostCardProps> = ({
  post,
  rank,
  currentUserId,
  isReacting,
  onReact,
  onEdit,
  onDelete,
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

  const handlePostClick = (e: React.MouseEvent) => {
    // Avoid navigation when clicking interactive elements
    const target = e.target as HTMLElement;
    if (
      target.closest("button") ||
      target.closest("a") ||
      target.closest(".comment-section") ||
      target.closest(".tag-badge")
    ) {
      return;
    }
    navigate(`${PATHS.POST_DETAIL.replace(":id", post.postId.toString())}`);
  };

  return (
    <article
      onClick={handlePostClick}
      className="bg-white dark:bg-[#1e1e1e] rounded-3xl border border-slate-200/80 dark:border-[#2a2a2a] p-4 sm:p-5 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden cursor-pointer"
    >
      {/* Top Rank Badge & Hot Score */}
      {rank && rank <= 3 && (
        <div className="absolute top-0 right-0 bg-gradient-to-l from-amber-500 to-orange-500 text-white text-[10px] font-black uppercase tracking-wider px-3 py-1 rounded-bl-2xl flex items-center gap-1 shadow-sm">
          <Flame className="w-3 h-3 fill-current" />
          <span>Top #{rank}</span>
        </div>
      )}

      <div className="flex items-start gap-3 sm:gap-4">
        {/* Author Avatar */}
        <div
          onClick={(e) => {
            e.stopPropagation();
            navigate(`${PATHS.PROFILE}?userId=${post.userId}`);
          }}
          className="cursor-pointer hover:opacity-80 transition-opacity mt-1 flex-shrink-0"
        >
          <SafeAvatar
            src={post.userAvatar}
            alt={post.username}
            className="w-10 h-10 sm:w-11 sm:h-11 rounded-full border border-slate-200 dark:border-neutral-700"
          />
        </div>

        {/* Post Body Container */}
        <div className="flex-1 min-w-0">
          {/* Header Row: Author, Username, Recency, Menu */}
          <div className="flex items-center justify-between gap-2 mb-1.5">
            <div className="flex items-center gap-2 flex-wrap min-w-0">
              <span
                onClick={(e) => {
                  e.stopPropagation();
                  navigate(`${PATHS.PROFILE}?userId=${post.userId}`);
                }}
                className="font-bold text-slate-900 dark:text-white text-sm sm:text-base hover:underline cursor-pointer truncate"
              >
                {post.username}
              </span>
              <span className="text-xs text-slate-400 dark:text-neutral-500 truncate">
                @{post.username}
              </span>
              <span className="text-slate-300 dark:text-neutral-600">·</span>
              <span className="text-xs text-slate-400 dark:text-neutral-500 whitespace-nowrap">
                {timeAgo(post.createdAt)}
              </span>
            </div>

            {/* Menu Dropdown Trigger (only if author) */}
            {isAuthor && (
              <div className="relative" onClick={(e) => e.stopPropagation()}>
                <button
                  onClick={() => setShowMenu((prev) => !prev)}
                  className="p-1 rounded-full text-slate-400 hover:text-slate-600 dark:hover:text-neutral-200 hover:bg-slate-100 dark:hover:bg-neutral-800 transition"
                  aria-label="Tùy chọn bài viết"
                >
                  <MoreHorizontal className="w-5 h-5" />
                </button>

                {showMenu && (
                  <div
                    className="absolute right-0 top-8 z-10 w-40 bg-white dark:bg-neutral-800 border border-slate-200 dark:border-neutral-700 rounded-2xl shadow-xl overflow-hidden py-1"
                    onMouseLeave={() => setShowMenu(false)}
                  >
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
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Topic Tags */}
          {post.topicSlugs && post.topicSlugs.length > 0 && (
            <div className="flex flex-wrap gap-1.5 mb-2.5">
              {post.topicSlugs.map((slug) => (
                <span
                  key={slug}
                  onClick={(e) => {
                    e.stopPropagation();
                    navigate(`${PATHS.DISCOVERY}?q=${encodeURIComponent(slug)}&mode=posts&type=topic`);
                  }}
                  className="tag-badge text-[11px] font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/60 px-2.5 py-0.5 rounded-full cursor-pointer transition"
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
