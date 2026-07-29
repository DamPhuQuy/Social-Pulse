import { useState, useRef, useEffect } from "react";
import { Activity, Flag, Loader2, MessageCircle, MoreHorizontal, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import { type UserProfile } from "@/features/profiles/infrastructure/api/userService";
import { 
  type CommentResponse, 
  reactComment, 
  createComment, 
  deleteComment, 
  getCommentReplies,
  updateComment,
} from "@/features/feed/infrastructure/api/commentService";
import ReportModal from "@/shared/components/ReportModal";

// ─── Time formatter ────────────────────────────────────────────────────────────
function timeAgo(dateStr: string): string {
  if (!dateStr) return "";
  const parsedDate = !dateStr.endsWith("Z") && !dateStr.includes("+")
    ? new Date(dateStr.includes("T") ? dateStr + "Z" : dateStr.replace(" ", "T") + "Z")
    : new Date(dateStr);
  const diff = (Date.now() - parsedDate.getTime()) / 1000;
  if (diff < 60) return `Vừa xong`;
  if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
  return `${Math.floor(diff / 86400)} ngày trước`;
}

interface CommentNodeProps {
  postId: number;
  comment: CommentResponse;
  depth: number;
  currentUser?: UserProfile | null;
  onCommentDeleted: (commentId: number) => void;
  onCommentCountDelta?: (delta: number) => void;
  // Used to pass down parent username for depth > 4 tagging
  parentUsername?: string;
}

export default function CommentNode({ 
  postId, 
  comment, 
  depth, 
  currentUser,
  onCommentDeleted,
  onCommentCountDelta,
  parentUsername 
}: CommentNodeProps) {
  
  // ─── Reactions & State ────────────────────────────────────────────────────────
  const [upvoteCount, setUpvoteCount] = useState(comment.upvoteCount);
  const [myReaction, setMyReaction] = useState<"UPVOTE" | "DOWNVOTE" | null>(comment.myReaction || null);
  const [commentContent, setCommentContent] = useState(comment.content);
  const [isEdited, setIsEdited] = useState(comment.edited);
  const [isReacting, setIsReacting] = useState(false);
  
  // ─── Input & Replies ──────────────────────────────────────────────────────────
  const [showReplyInput, setShowReplyInput] = useState(false);
  const [replyContent, setReplyContent] = useState("");
  const [isSubmittingReply, setIsSubmittingReply] = useState(false);
  const replyInputRef = useRef<HTMLTextAreaElement>(null);

  // ─── Sub-Replies ──────────────────────────────────────────────────────────────
  const [replies, setReplies] = useState<CommentResponse[]>([]);
  const [showReplies, setShowReplies] = useState(false);
  const [repliesLoading, setRepliesLoading] = useState(false);
  const [repliesLoaded, setRepliesLoaded] = useState(false);

  // ─── Options dropdown ─────────────────────────────────────────────────────────
  const [showOptions, setShowOptions] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [isSavingEdit, setIsSavingEdit] = useState(false);
  const [showReportModal, setShowReportModal] = useState(false);

  useEffect(() => {
    setUpvoteCount(comment.upvoteCount);
    setMyReaction(comment.myReaction || null);
    setCommentContent(comment.content);
    setEditContent(comment.content);
    setIsEdited(comment.edited);
  }, [comment.upvoteCount, comment.myReaction, comment.content, comment.edited]);

  // ─── 1. AUTO-FOCUS INPUT EFFECT ───────────────────────────────────────────────
  useEffect(() => {
    if (showReplyInput && replyInputRef.current) {
      replyInputRef.current.focus();
      // Scroll slightly if needed to keep in viewport
      replyInputRef.current.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }
  }, [showReplyInput]);

  // ─── 2. OPTIMISTIC UPDATE UPVOTE ──────────────────────────────────────────────
  const handlePulse = async () => {
    if (isReacting) return;

    const prevReaction = myReaction;
    const prevCount = upvoteCount;
    setIsReacting(true);

    if (myReaction === "UPVOTE") {
      setMyReaction(null);
      setUpvoteCount((prev) => Math.max(0, prev - 1));
    } else {
      setMyReaction("UPVOTE");
      setUpvoteCount((prev) => prev + 1);
    }

    const res = await reactComment(postId, comment.id, "UPVOTE");
    if (!res.ok) {
      // Revert if API fails
      setMyReaction(prevReaction);
      setUpvoteCount(prevCount);
      toast.error(res.message || "Không thể thích bình luận.");
    }
    setIsReacting(false);
  };

  // ─── 3. SUB-REPLIES LAZY LOAD ─────────────────────────────────────────────────
  const handleLoadReplies = async () => {
    if (repliesLoaded) {
      setShowReplies(!showReplies);
      return;
    }
    setRepliesLoading(true);
    const res = await getCommentReplies(postId, comment.id);
    setRepliesLoading(false);
    if (res.ok && res.data) {
      setReplies(res.data);
      setRepliesLoaded(true);
      setShowReplies(true);
    } else {
      toast.error(res.message || "Không thể tải phản hồi.");
    }
  };

  // ─── 4. SUBMIT REPLY ──────────────────────────────────────────────────────────
  const handleSubmitReply = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSubmittingReply || !replyContent.trim()) return;

    setIsSubmittingReply(true);
    const res = await createComment(postId, replyContent.trim(), comment.id);
    setIsSubmittingReply(false);

    if (res.ok && res.data) {
      setReplyContent("");
      setShowReplyInput(false);

      // Add to loaded replies or trigger load
      const newReply: CommentResponse = {
        id: res.data.id,
        user: {
          id: currentUser?.userId || 0,
          username: currentUser?.username || "me",
          avatarUrl: currentUser?.avatarUrl || null,
        },
        content: res.data.content,
        createdAt: res.data.createdAt,
        edited: false,
        upvoteCount: 0,
        downvoteCount: 0,
        replyCount: 0,
        myReaction: null,
      };

      setReplies((prev) => [newReply, ...prev]);
      setShowReplies(true);
      setRepliesLoaded(true);
      onCommentCountDelta?.(1);
    } else {
      toast.error(res.message || "Lỗi khi phản hồi.");
    }
  };

  // ─── 5. DELETE COMMENT ────────────────────────────────────────────────────────
  const handleDelete = async () => {
    if (isDeleting) return;
    if (!window.confirm("Bạn có chắc chắn muốn xóa bình luận này không?")) return;
    setIsDeleting(true);
    const res = await deleteComment(postId, comment.id);
    setIsDeleting(false);
    if (res.ok) {
      toast.success("Đã xóa bình luận.");
      onCommentDeleted(comment.id);
    } else {
      toast.error(res.message || "Không thể xóa bình luận.");
    }
  };

  const handleUpdate = async () => {
    if (isSavingEdit || !editContent.trim() || editContent.trim() === commentContent.trim()) {
      setIsEditing(false);
      setEditContent(commentContent);
      return;
    }

    setIsSavingEdit(true);
    const res = await updateComment(postId, comment.id, editContent.trim());
    setIsSavingEdit(false);
    if (!res.ok || !res.data) {
      toast.error(res.message || "Không thể cập nhật bình luận.");
      return;
    }

    setCommentContent(res.data.content);
    setEditContent(res.data.content);
    setIsEdited(true);
    setIsEditing(false);
    toast.success("Đã cập nhật bình luận.");
  };

  // ─── 6. DEPTH & INDENTATION SETTINGS ──────────────────────────────────────────
  // Cap depth indentation at level 4.
  const isCapped = depth >= 4;
  const showGuideLine = depth > 0 && depth <= 4;

  return (
    <div className="relative flex flex-col w-full group/node">
      {/* Visual Guide Line for Threaded Indent */}
      {showGuideLine && (
        <div 
          className="absolute left-[-24px] top-0 bottom-0 w-0.5 border-l border-slate-200 dark:border-neutral-800 group-hover/node:border-slate-400 dark:group-hover/node:border-neutral-600 transition-colors duration-200" 
          style={{ height: "100%" }}
        />
      )}

      {/* Main Comment Node Container */}
      <div 
        className="flex gap-3 w-full p-2.5 rounded-2xl hover:bg-slate-50/50 dark:hover:bg-neutral-900/30 transition-colors duration-200 relative group/card"
        style={{ paddingLeft: isCapped ? "0px" : "12px" }}
      >
        {/* User Avatar */}
        <div className="w-9 h-9 rounded-full overflow-hidden flex-shrink-0 border border-slate-200 dark:border-neutral-800">
          <SafeAvatar src={comment.user.avatarUrl} alt={comment.user.username} />
        </div>

        {/* Comment Content Area */}
        <div className="flex-1 flex flex-col min-w-0">
          {/* Header Metadata */}
          <div className="flex items-center gap-2 flex-wrap">
            <span className="font-semibold text-slate-800 dark:text-neutral-200 text-sm">
              {comment.user.username}
            </span>
            {comment.user.id === currentUser?.userId && (
              <span className="text-[10px] bg-slate-100 dark:bg-neutral-800 text-slate-500 px-1.5 py-0.5 rounded-full font-medium">
                Tác giả
              </span>
            )}
            <span className="text-xs text-slate-400 dark:text-neutral-500">
              {timeAgo(comment.createdAt)}
            </span>
          </div>

          {/* Comment Bubble Content */}
          {isEditing ? (
            <div className="mt-2">
              <textarea
                rows={3}
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                className="w-full resize-none rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none focus:border-blue-500 dark:border-neutral-800 dark:bg-neutral-900 dark:text-white"
              />
              <div className="mt-2 flex gap-2">
                <button onClick={handleUpdate} disabled={isSavingEdit || !editContent.trim()} className="rounded-xl bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50 dark:bg-white dark:text-slate-900">
                  {isSavingEdit ? "Đang lưu..." : "Lưu"}
                </button>
                <button onClick={() => { setIsEditing(false); setEditContent(commentContent); }} className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold dark:border-neutral-800">
                  Hủy
                </button>
              </div>
            </div>
          ) : (
            <p className="text-slate-700 dark:text-neutral-300 text-sm mt-1 leading-relaxed break-words">
              {isCapped && parentUsername && (
                <span className="text-blue-500 font-semibold mr-1.5 dark:text-cyan-400">
                  @{parentUsername}
                </span>
              )}
              {commentContent}
              {isEdited ? <span className="ml-2 text-[11px] text-slate-400 dark:text-neutral-500">(đã sửa)</span> : null}
            </p>
          )}

          {/* Action Row - Optimistic Pulse & Reply toggle */}
          <div className="flex items-center gap-4 mt-2 select-none">
            {/* Upvote/Pulse button */}
            <button
              disabled={isReacting}
              onClick={handlePulse}
              className={`flex items-center gap-1.5 text-xs transition-colors duration-200 p-1 -m-1 rounded-md hover:bg-slate-100 dark:hover:bg-neutral-800 ${
                myReaction === "UPVOTE"
                  ? "text-slate-900 dark:text-white font-semibold"
                  : "text-slate-400 dark:text-neutral-500 hover:text-slate-600"
              }`}
            >
              <Activity className={`w-3.5 h-3.5 ${myReaction === "UPVOTE" ? "stroke-[2.5px]" : "stroke-[1.5px]"}`} />
              <span>{upvoteCount > 0 ? upvoteCount : "Pulse"}</span>
            </button>

            {/* Reply toggle */}
            <button
              onClick={() => setShowReplyInput(!showReplyInput)}
              className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-slate-600 dark:text-neutral-500 dark:hover:text-neutral-400 transition-colors duration-200 p-1 -m-1 rounded-md hover:bg-slate-100 dark:hover:bg-neutral-800"
            >
              <MessageCircle className="w-3.5 h-3.5 stroke-[1.5px]" />
              <span>Trả lời</span>
            </button>

            {/* 3. OPTION MENU FOR OWNER (HOVER EFFECT) */}
            <div className="relative opacity-0 group-hover/card:opacity-100 transition-opacity duration-200">
                <button
                  onClick={() => setShowOptions(!showOptions)}
                  className="p-1 rounded-full text-slate-400 hover:bg-slate-100 dark:text-neutral-500 dark:hover:bg-neutral-800 dark:hover:text-neutral-400"
                >
                  <MoreHorizontal className="w-3.5 h-3.5 stroke-[1.5px]" />
                </button>

                {showOptions && (
                  <div className="absolute left-0 mt-1 w-28 bg-white dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800 rounded-xl shadow-lg py-1 z-30">
                    {comment.user.id === currentUser?.userId ? (
                      <>
                        <button
                          onClick={() => {
                            setShowOptions(false);
                            setIsEditing(true);
                          }}
                          className="w-full text-left px-3 py-1.5 text-xs text-slate-600 hover:bg-slate-50 dark:text-neutral-300 dark:hover:bg-neutral-800 flex items-center gap-2"
                        >
                          <Pencil className="w-3.5 h-3.5" />
                          <span>Sửa</span>
                        </button>
                        <button
                          onClick={() => {
                            setShowOptions(false);
                            handleDelete();
                          }}
                          disabled={isDeleting}
                          className="w-full text-left px-3 py-1.5 text-xs text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 flex items-center gap-2"
                        >
                          {isDeleting ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <Trash2 className="w-3.5 h-3.5" />
                          )}
                          <span>Xóa</span>
                        </button>
                      </>
                    ) : (
                      <button
                        onClick={() => {
                          setShowOptions(false);
                          setShowReportModal(true);
                        }}
                        className="w-full text-left px-3 py-1.5 text-xs text-red-500 hover:bg-red-50 dark:hover:bg-red-950/20 flex items-center gap-2"
                      >
                        <Flag className="w-3.5 h-3.5" />
                        <span>Báo cáo</span>
                      </button>
                    )}
                  </div>
                )}
              </div>
          </div>
        </div>
      </div>

      {/* Reply input field (Auto-focused when active) */}
      {showReplyInput && (
        <form 
          onSubmit={handleSubmitReply}
          className="flex gap-2.5 items-end mt-2 ml-4 p-2 bg-slate-50 dark:bg-neutral-900/50 rounded-2xl border border-slate-100 dark:border-neutral-800/40 relative z-10"
          style={{ marginLeft: isCapped ? "0px" : "36px" }}
        >
          <textarea
            ref={replyInputRef}
            rows={1}
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            placeholder={`Phản hồi ${comment.user.username}...`}
            className="flex-1 resize-none bg-transparent border-0 focus:ring-0 text-slate-700 dark:text-neutral-200 text-sm py-1 px-2 focus:outline-none placeholder-slate-400 dark:placeholder-neutral-600 max-h-24 overflow-y-auto"
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSubmitReply(e);
              }
            }}
          />
          <button
            type="submit"
            disabled={isSubmittingReply || !replyContent.trim()}
            className="px-4 py-1.5 text-xs font-semibold bg-slate-900 hover:bg-slate-800 dark:bg-white dark:hover:bg-slate-100 text-white dark:text-slate-900 rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed h-8 flex items-center justify-center shrink-0"
          >
            {isSubmittingReply ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              "Gửi"
            )}
          </button>
        </form>
      )}

      {/* Lazy Loaded Nested Replies List */}
      <div 
        className="flex flex-col gap-2 relative mt-2" 
        style={{ paddingLeft: isCapped ? "0px" : "36px" }}
      >
        {/* Load replies dynamic button */}
        {comment.replyCount > 0 && !repliesLoaded && (
          <button
            onClick={handleLoadReplies}
            disabled={repliesLoading}
            className="flex items-center gap-2 text-xs font-semibold text-slate-500 hover:text-slate-700 dark:text-neutral-400 dark:hover:text-neutral-200 py-1 pl-3 self-start transition-colors duration-200"
          >
            {repliesLoading ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin text-slate-400" />
            ) : (
              <span className="w-5 h-px bg-slate-300 dark:bg-neutral-800 inline-block mr-1" />
            )}
            <span>Xem thêm {comment.replyCount} phản hồi...</span>
          </button>
        )}

        {/* Cây đệ quy - Gọi lại chính nó */}
        {showReplies && replies.length > 0 && (
          <div className="flex flex-col gap-3.5 mt-1">
            {replies.map((reply) => (
              <CommentNode
                key={reply.id}
                postId={postId}
                comment={reply}
                depth={depth + 1}
                currentUser={currentUser}
                parentUsername={comment.user.username}
                onCommentDeleted={(deletedId) => {
                  setReplies((prev) => prev.filter((r) => r.id !== deletedId));
                  onCommentCountDelta?.(-1);
                }}
                onCommentCountDelta={onCommentCountDelta}
              />
            ))}
          </div>
        )}
      </div>
      <ReportModal
        isOpen={showReportModal}
        targetType="COMMENT"
        targetId={comment.id}
        title="bình luận"
        onClose={() => setShowReportModal(false)}
      />
    </div>
  );
}
