import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  ChevronUp,
  MessageSquare,
  Bookmark,
  Share2,
  ExternalLink,
  MoreHorizontal,
  Edit3,
  Trash2,
  Globe,
} from "lucide-react";
import { PATHS } from "@/shared/constants/paths";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { PostMedia } from "@/features/feed/presentation/components/PostMedia";
import CommentSection from "@/features/feed/presentation/components/CommentSection";
import { timeAgo } from "@/core/utils/dateUtils";
import type { FeedItem, PulseReaction } from "@/features/feed/infrastructure/api/postService";

export interface DevCardProps {
  post: FeedItem;
  currentUserId?: number;
  isReacting?: boolean;
  onReact: (postId: number, type: PulseReaction) => void;
  onToggleBookmark: (postId: number) => void;
  onEdit: (post: FeedItem) => void;
  onDelete: (postId: number) => void;
  onShare?: (post: FeedItem) => void;
}

export const DevCard: React.FC<DevCardProps> = ({
  post,
  currentUserId,
  isReacting,
  onReact,
  onToggleBookmark,
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

  const handleCardClick = (e: React.MouseEvent) => {
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
      onClick={handleCardClick}
      className="bg-white dark:bg-[#18181b] rounded-2xl border border-slate-200/80 dark:border-zinc-800 p-5 shadow-sm hover:border-slate-300 dark:hover:border-zinc-700 transition-all relative overflow-hidden cursor-pointer group"
    >
      {/* ── 1. Header: Source / Author & Recency ─────────────────────────────── */}
      <div className="flex items-center justify-between gap-3 mb-3 text-xs text-slate-500 dark:text-zinc-400">
        <div className="flex items-center gap-2 flex-wrap min-w-0">
          {post.sourceDomain ? (
            <div className="flex items-center gap-1.5 font-medium text-slate-700 dark:text-zinc-300 bg-slate-100 dark:bg-zinc-800/80 px-2.5 py-1 rounded-md">
              <Globe className="w-3.5 h-3.5 text-blue-500" />
              <span className="font-mono text-[11px]">{post.sourceDomain}</span>
            </div>
          ) : (
            <div
              onClick={(e) => {
                e.stopPropagation();
                navigate(`${PATHS.PROFILE}?userId=${post.userId}`);
              }}
              className="flex items-center gap-1.5 hover:underline cursor-pointer"
            >
              <SafeAvatar src={post.userAvatar} alt={post.username} className="w-5 h-5 rounded-full" />
              <span className="font-semibold text-slate-800 dark:text-zinc-200">@{post.username}</span>
            </div>
          )}

          <span>·</span>
          <span>{timeAgo(post.createdAt)}</span>

          {post.sourceUrl && (
            <a
              href={post.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              onClick={(e) => e.stopPropagation()}
              className="inline-flex items-center gap-1 text-blue-600 dark:text-blue-400 hover:underline ml-1"
              title="Mở bài viết gốc"
            >
              <span>Nguồn</span>
              <ExternalLink className="w-3 h-3" />
            </a>
          )}
        </div>

        {/* Action Menu for Author */}
        {isAuthor && (
          <div className="relative" onClick={(e) => e.stopPropagation()}>
            <button
              onClick={() => setShowMenu((prev) => !prev)}
              className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-zinc-200 hover:bg-slate-100 dark:hover:bg-zinc-800 transition"
              aria-label="Tùy chọn bài viết"
            >
              <MoreHorizontal className="w-4 h-4" />
            </button>

            {showMenu && (
              <div
                className="absolute right-0 top-7 z-20 w-36 bg-white dark:bg-zinc-900 border border-slate-200 dark:border-zinc-800 rounded-xl shadow-xl overflow-hidden py-1"
                onMouseLeave={() => setShowMenu(false)}
              >
                <button
                  onClick={() => {
                    setShowMenu(false);
                    onEdit(post);
                  }}
                  className="w-full text-left px-3 py-1.5 text-xs font-medium text-slate-700 dark:text-zinc-200 hover:bg-slate-50 dark:hover:bg-zinc-800 flex items-center gap-2"
                >
                  <Edit3 className="w-3.5 h-3.5" /> Chỉnh sửa
                </button>
                <button
                  onClick={() => {
                    setShowMenu(false);
                    onDelete(post.postId);
                  }}
                  className="w-full text-left px-3 py-1.5 text-xs font-medium text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 flex items-center gap-2"
                >
                  <Trash2 className="w-3.5 h-3.5" /> Xóa bài
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* ── 2. Title (Bold Dev Headline) ────────────────────────────────────── */}
      <h2 className="text-base sm:text-lg font-bold text-slate-900 dark:text-white leading-snug mb-2 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
        {post.title || post.content.slice(0, 60)}
      </h2>

      {/* ── 3. Summary / Excerpt ────────────────────────────────────────────── */}
      {post.summary ? (
        <p className="text-sm text-slate-600 dark:text-zinc-400 line-clamp-2 leading-relaxed mb-3">
          {post.summary}
        </p>
      ) : post.content && post.title && post.content !== post.title ? (
        <p className="text-sm text-slate-600 dark:text-zinc-400 line-clamp-2 leading-relaxed mb-3">
          {post.content}
        </p>
      ) : null}

      {/* ── 4. Media Thumbnail (Optional) ──────────────────────────────────── */}
      {post.imageUrl && (
        <div className="mb-3 rounded-xl overflow-hidden border border-slate-100 dark:border-zinc-800 max-h-64">
          <PostMedia mediaUrl={post.imageUrl} />
        </div>
      )}

      {/* ── 5. Tech Tag Badges ──────────────────────────────────────────────── */}
      {post.topicSlugs && post.topicSlugs.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-4">
          {post.topicSlugs.map((slug) => (
            <span
              key={slug}
              onClick={(e) => {
                e.stopPropagation();
                navigate(`${PATHS.DISCOVERY}?q=${encodeURIComponent(slug)}&mode=posts&type=topic`);
              }}
              className="tag-badge text-[11px] font-mono font-medium text-slate-600 dark:text-zinc-300 bg-slate-100 dark:bg-zinc-800 hover:bg-slate-200 dark:hover:bg-zinc-700 px-2.5 py-0.5 rounded-md cursor-pointer transition"
            >
              #{slug}
            </span>
          ))}
        </div>
      )}

      {/* ── 6. Bottom Developer Action Bar ──────────────────────────────────── */}
      <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-zinc-800/80">
        {/* Left: Upvote Counter */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => onReact(post.postId, isUpvoted ? "NONE" : "UPVOTE")}
            disabled={isReacting}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              isUpvoted
                ? "bg-blue-600 text-white shadow-sm"
                : "bg-slate-100 dark:bg-zinc-800 text-slate-600 dark:text-zinc-300 hover:bg-slate-200 dark:hover:bg-zinc-700"
            }`}
            title="Upvote bài viết"
          >
            <ChevronUp className={`w-4 h-4 ${isUpvoted ? "stroke-[3px]" : ""}`} />
            <span>{post.upvoteCount}</span>
          </button>

          {/* Comment Counter Button */}
          <button
            onClick={() => setShowComments((prev) => !prev)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-500 dark:text-zinc-400 hover:bg-slate-100 dark:hover:bg-zinc-800 transition"
            title="Thảo luận"
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span>{cmtCount}</span>
          </button>
        </div>

        {/* Right: Save/Bookmark & Share */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => onToggleBookmark(post.postId)}
            className={`p-2 rounded-lg text-xs transition-colors ${
              post.isBookmarked
                ? "text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40"
                : "text-slate-400 hover:text-slate-600 dark:hover:text-zinc-200 hover:bg-slate-100 dark:hover:bg-zinc-800"
            }`}
            title={post.isBookmarked ? "Bỏ lưu bài viết" : "Lưu vào danh sách đọc"}
          >
            <Bookmark className={`w-4 h-4 ${post.isBookmarked ? "fill-current" : ""}`} />
          </button>

          {onShare && (
            <button
              onClick={() => onShare(post)}
              className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-zinc-200 hover:bg-slate-100 dark:hover:bg-zinc-800 transition"
              title="Chia sẻ bài viết"
            >
              <Share2 className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* ── 7. Expandable Discussion Section ────────────────────────────────── */}
      {showComments && (
        <div className="mt-4 pt-4 border-t border-slate-100 dark:border-zinc-800 comment-section">
          <CommentSection
            postId={post.postId}
            initialCmtCount={cmtCount}
            onCommentCountChange={(count) => setCmtCount(count)}
          />
        </div>
      )}
    </article>
  );
};
