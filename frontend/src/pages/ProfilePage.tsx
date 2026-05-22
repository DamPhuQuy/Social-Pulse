import CommentSection from "@/components/comment/CommentSection";
import CreatePostModal from "@/components/post/CreatePostModal";
import AppHeader from "@/components/social/AppHeader";
import AppSidebar from "@/components/social/AppSidebar";
import BottomNavBar from "@/components/social/BottomNavBar";
import ReportModal from "@/components/social/ReportModal";
import UserListModal from "@/components/social/UserListModal";
import { PATHS } from "@/constants/paths";
import { useAuth } from "@/hooks/useAuth";
import {
  deletePost,
  reactPost,
  uploadMedia,
  type Privacy,
  type PulseReaction,
} from "@/services/post/postService";
import {
  blockUser,
  checkIsBlocked,
  unblockUser,
} from "@/services/social/blockService";
import {
  createBookmark,
  deleteBookmark,
  getBookmarks,
} from "@/services/social/bookmarkService";
import {
  followUser,
  getFollowers,
  getFollowing,
  unfollowUser,
  type UserSummary,
} from "@/services/social/followService";
import {
  getMyProfile,
  getUserPosts,
  getUserProfile,
  updateProfile,
  type UserPost,
  type UserProfile,
} from "@/services/user/userService";
import {
  Activity,
  Bookmark,
  CalendarDays,
  Camera,
  Crop,
  Edit3,
  Eye,
  Loader2,
  MessageCircle,
  MessageSquare,
  MoreHorizontal,
  MousePointerClick,
  Share2,
  Trash2,
  UserX,
  X,
} from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";

// Utility imports
import { timeAgo } from "@/lib/dateUtils";
import { nextPostPulseState } from "@/lib/postUtils";

// Component imports
import { SafeAvatar } from "@/components/ui/SafeAvatar";
import { PostMedia } from "@/components/post/PostMedia";

interface ProfilePostProps {
  post: UserPost;
  displayName: string;
  handleReact: (postId: number, type: PulseReaction) => void;
  isReacting: boolean;
  currentUserId?: number;
  onEdit: (post: UserPost) => void;
  onDelete: (postId: number) => void;
  isBookmarked: boolean;
  onToggleBookmark: (postId: number) => void;
  onReport: (postId: number) => void;
  onShare?: (post: UserPost) => void;
}

function ProfilePost({
  post,
  displayName,
  handleReact,
  isReacting,
  currentUserId,
  onEdit,
  onDelete,
  isBookmarked,
  onToggleBookmark,
  onReport,
  onShare,
}: ProfilePostProps) {
  const [showComments, setShowComments] = useState(false);
  const [cmtCount, setCmtCount] = useState(post.cmtCount);
  const [showMenu, setShowMenu] = useState(false);
  const navigate = useNavigate();
  const isUpvoted = post.myVote === 1;
  const isAuthor = currentUserId === post.userId;

  useEffect(() => {
    setCmtCount(post.cmtCount);
  }, [post.cmtCount]);

  return (
    <article className="bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] rounded-2xl p-5 shadow-[0_4px_20px_rgba(0,0,0,0.03)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.4)] hover:border-slate-300 dark:hover:border-neutral-700 hover:shadow-[0_8px_30px_rgba(0,0,0,0.04)] dark:hover:shadow-[0_8px_35px_rgba(0,0,0,0.5)] transition-all duration-300">
      <div className="flex gap-4">
        <div className="shrink-0 w-11 h-11 rounded-full overflow-hidden bg-slate-100 dark:bg-neutral-850">
          <SafeAvatar src={post.userAvatar} alt={post.username} />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between mb-1">
            <div className="flex items-center gap-2 truncate">
              <span className="font-bold text-[16px] text-slate-800 dark:text-[#e4e6eb] hover:underline truncate">
                {displayName}
              </span>
              <span className="text-slate-500 dark:text-neutral-400 text-sm">
                · {timeAgo(post.createdAt)}
              </span>
            </div>
            <div className="relative shrink-0">
              <button
                onClick={() => setShowMenu((value) => !value)}
                className="text-slate-400 dark:text-neutral-500 hover:text-blue-500 p-1.5 rounded-full transition-colors"
              >
                <MoreHorizontal className="w-4 h-4" />
              </button>
              {showMenu && (
                <div className="absolute right-0 top-8 z-20 w-44 overflow-hidden rounded-xl border border-slate-200 dark:border-neutral-800 bg-white dark:bg-neutral-900 shadow-xl">
                  {isAuthor ? (
                    <>
                      <button
                        onClick={() => {
                          setShowMenu(false);
                          onEdit(post);
                        }}
                        className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold hover:bg-slate-50 dark:hover:bg-neutral-800"
                      >
                        <Edit3 className="w-4 h-4" /> Chỉnh sửa
                      </button>
                      <button
                        onClick={() => {
                          setShowMenu(false);
                          onDelete(post.postId);
                        }}
                        className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold text-red-600 hover:bg-red-50 dark:hover:bg-red-500/10"
                      >
                        <Trash2 className="w-4 h-4" /> Xóa bài viết
                      </button>
                    </>
                  ) : (
                    <button
                      onClick={() => {
                        setShowMenu(false);
                        onReport(post.postId);
                      }}
                      className="flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm font-semibold text-red-600 hover:bg-red-50 dark:hover:bg-red-500/10"
                    >
                      <Trash2 className="w-4 h-4" /> Báo cáo
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          <p className="text-[15px] leading-relaxed mb-3 whitespace-pre-line break-words text-gray-800 dark:text-neutral-200">
            {post.content}
          </p>

          <PostMedia urls={post.imageUrl ? post.imageUrl.split(",") : []} variant="profile" />

          {post.topicSlugs?.length > 0 && (
            <div className="mb-3 flex flex-wrap gap-2">
              {post.topicSlugs.map((topic) => (
                <span
                  key={topic}
                  className="rounded-full bg-slate-100 dark:bg-neutral-800 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:text-neutral-400"
                >
                  #{topic}
                </span>
              ))}
            </div>
          )}

          <div className="flex items-center justify-between text-gray-500 dark:text-neutral-500 max-w-xs mt-4">
            <button
              disabled={isReacting}
              onClick={(e) => {
                e.stopPropagation();
                handleReact(post.postId, "UPVOTE");
              }}
              className={`flex items-center gap-2 transition-colors group ${isUpvoted ? "text-blue-600 dark:text-blue-400" : "hover:text-slate-900 dark:hover:text-white"}`}
            >
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <Activity
                  className={`w-5 h-5 ${isUpvoted ? "stroke-[2.5px]" : "stroke-2"}`}
                />
              </div>
              <span className="text-sm">
                {post.upvoteCount > 0 ? post.upvoteCount : ""}
              </span>
            </button>

            <button
              onClick={(e) => {
                e.stopPropagation();
                setShowComments(!showComments);
              }}
              className={`flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group ${showComments ? "text-slate-900 dark:text-white font-bold" : ""}`}
            >
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <MessageCircle
                  className={`w-5 h-5 ${showComments ? "stroke-[2.5px]" : "stroke-2"}`}
                />
              </div>
              <span className="text-sm">{cmtCount > 0 ? cmtCount : ""}</span>
            </button>

            <button
              onClick={() => onToggleBookmark(post.postId)}
              className={`flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group ${isBookmarked ? "text-blue-600 dark:text-blue-400" : ""}`}
            >
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <Bookmark
                  className={`w-5 h-5 stroke-2 ${isBookmarked ? "fill-current" : ""}`}
                />
              </div>
            </button>

            <button
              onClick={(e) => {
                e.stopPropagation();
                if (!currentUserId) {
                  toast.error("Vui lòng đăng nhập để chia sẻ bài viết.");
                  navigate(PATHS.LOGIN);
                  return;
                }
                onShare?.(post);
              }}
              className="flex items-center gap-2 hover:text-slate-900 dark:hover:text-white transition-colors group"
            >
              <div className="p-1.5 rounded-full group-hover:bg-slate-100 dark:group-hover:bg-neutral-800">
                <Share2 className="w-5 h-5 stroke-2" />
              </div>
            </button>
          </div>

          {showComments && (
            <CommentSection
              postId={post.postId}
              initialCmtCount={cmtCount}
              onCommentCountChange={setCmtCount}
            />
          )}
        </div>
      </div>
    </article>
  );
}

export default function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const navigate = useNavigate();
  const { accessToken } = useAuth();

  // Core States
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [myProfile, setMyProfile] = useState<UserProfile | null>(null);
  const [posts, setPosts] = useState<UserPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [postsLoading, setPostsLoading] = useState(false);
  const [reactingPostIds, setReactingPostIds] = useState<Set<number>>(
    () => new Set(),
  );
  const [bookmarkingPostIds, setBookmarkingPostIds] = useState<Set<number>>(
    () => new Set(),
  );
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(
    () => new Set(),
  );
  const [editingPost, setEditingPost] = useState<UserPost | null>(null);
  const [sharingPost, setSharingPost] = useState<UserPost | null>(null);
  const [followLoading, setFollowLoading] = useState(false);
  const [followersList, setFollowersList] = useState<UserSummary[]>([]);
  const [followingList, setFollowingList] = useState<UserSummary[]>([]);
  const [showFollowersModal, setShowFollowersModal] = useState(false);
  const [showFollowingModal, setShowFollowingModal] = useState(false);
  const [reportPostId, setReportPostId] = useState<number | null>(null);

  // Blocking states
  const [isBlocked, setIsBlocked] = useState(false);
  const [blockLoading, setBlockLoading] = useState(false);

  // Edit Profile States
  const [showEditModal, setShowEditModal] = useState(false);
  const [editDisplayName, setEditDisplayName] = useState("");
  const [editBio, setEditBio] = useState("");

  // Cover & Avatar Upload States
  const [uploadingCover, setUploadingCover] = useState(false);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const coverInputRef = useRef<HTMLInputElement>(null);
  const avatarInputRef = useRef<HTMLInputElement>(null);

  // Avatar Circular Cropper States
  const [avatarPreviewSrc, setAvatarPreviewSrc] = useState<string | null>(null);
  const [originalAvatarFile, setOriginalAvatarFile] = useState<File | null>(
    null,
  );
  const [previewAspect, setPreviewAspect] = useState<number>(1);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const cropperImgRef = useRef<HTMLImageElement>(null);
  const [viewingImageUrl, setViewingImageUrl] = useState<string | null>(null);

  useEffect(() => {
    getMyProfile().then((res) => {
      if (res.ok && res.data) {
        setMyProfile(res.data);
      }
    });
    getBookmarks(0, 100).then((res) => {
      if (res.ok && res.data) {
        setBookmarkedPostIds(
          new Set((res.data.items ?? []).map((item) => item.postId)),
        );
      }
    });
  }, []);

  const loadData = useCallback(async () => {
    setLoading(true);
    let profileData: UserProfile | undefined;

    if (username) {
      const res = await getUserProfile(username);
      if (res.ok && res.data) profileData = res.data;
      else toast.error(res.message);
    } else {
      const res = await getMyProfile();
      if (res.ok && res.data) {
        profileData = res.data;
        setMyProfile(res.data);
      } else toast.error(res.message);
    }

    if (profileData) {
      setProfile(profileData);
      setEditDisplayName(profileData.displayName || "");
      setEditBio(profileData.bio || "");

      // Check block status
      if (username) {
        const blockRes = await checkIsBlocked(profileData.userId);
        if (blockRes.ok && blockRes.data !== undefined) {
          setIsBlocked(blockRes.data);
        }
      }

      setPostsLoading(true);
      const postRes = await getUserPosts(profileData.userId, 0, 50);
      if (postRes.ok && postRes.data) {
        setPosts(postRes.data.items || []);
      } else {
        toast.error(postRes.message);
      }
      setPostsLoading(false);
    }
    setLoading(false);
  }, [username]);

  const handleToggleBlock = async () => {
    if (!profile) return;
    setBlockLoading(true);
    if (isBlocked) {
      const res = await unblockUser(profile.userId);
      if (res.ok) {
        toast.success(
          `Đã hủy chặn ${profile.displayName || profile.username}!`,
        );
        setIsBlocked(false);
        loadData();
      } else {
        toast.error(res.message ?? "Hủy chặn thất bại.");
      }
    } else {
      if (
        window.confirm(
          `Bạn có chắc chắn muốn chặn ${profile.displayName || profile.username}? Các mối quan hệ Theo dõi sẽ bị hủy bỏ tự động.`,
        )
      ) {
        const res = await blockUser(profile.userId);
        if (res.ok) {
          toast.success(
            `Đã chặn thành công ${profile.displayName || profile.username}!`,
          );
          setIsBlocked(true);
          setPosts([]);
          setProfile((prev) => (prev ? { ...prev, isFollowing: false } : null));
        } else {
          toast.error(res.message ?? "Chặn thất bại.");
        }
      }
    }
    setBlockLoading(false);
  };

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleReact = async (postId: number, type: PulseReaction) => {
    if (!accessToken) {
      toast.error("Vui lòng đăng nhập trước.");
      return;
    }

    if (reactingPostIds.has(postId)) return;

    const previousPost = posts.find((post) => post.postId === postId);
    if (!previousPost) return;

    setReactingPostIds((prev) => new Set(prev).add(postId));

    setPosts((prevPosts) =>
      prevPosts.map((post) => {
        if (post.postId === postId) {
          return nextPostPulseState(post);
        }
        return post;
      }),
    );

    try {
      const res = await reactPost({ postId, reactionType: type });
      if (!res.ok) {
        setPosts((prevPosts) =>
          prevPosts.map((post) =>
            post.postId === postId ? previousPost : post,
          ),
        );
        toast.error(res.message ?? "Thả cảm xúc thất bại.");
      }
    } catch (err) {
      console.error(err);
      setPosts((prevPosts) =>
        prevPosts.map((post) => (post.postId === postId ? previousPost : post)),
      );
    } finally {
      setReactingPostIds((prev) => {
        const next = new Set(prev);
        next.delete(postId);
        return next;
      });
    }
  };

  const handlePostUpdated = (updated: {
    postId: number;
    content: string;
    imageUrl: string | null;
    topicSlugs: string[];
    privacy: Privacy;
    updatedAt: string;
  }) => {
    setPosts((prevPosts) =>
      prevPosts.map((post) =>
        post.postId === updated.postId
          ? {
              ...post,
              content: updated.content,
              imageUrl: updated.imageUrl,
              topicSlugs: updated.topicSlugs,
              privacy: updated.privacy,
              updatedAt: updated.updatedAt,
              createdAt: updated.updatedAt,
            }
          : post,
      ),
    );
    setEditingPost(null);
  };

  const handleDeletePost = async (postId: number) => {
    const previousPosts = posts;
    setPosts((prevPosts) => prevPosts.filter((post) => post.postId !== postId));

    const result = await deletePost(postId);
    if (!result.ok) {
      setPosts(previousPosts);
      toast.error(result.message ?? "Xóa bài viết thất bại.");
      return;
    }
    toast.success("Đã xóa bài viết.");
  };

  const handleToggleBookmark = async (postId: number) => {
    if (bookmarkingPostIds.has(postId)) return;

    const wasBookmarked = bookmarkedPostIds.has(postId);
    setBookmarkingPostIds((prev) => new Set(prev).add(postId));
    setBookmarkedPostIds((prev) => {
      const next = new Set(prev);
      if (wasBookmarked) next.delete(postId);
      else next.add(postId);
      return next;
    });

    const res = wasBookmarked
      ? await deleteBookmark(postId)
      : await createBookmark(postId);
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
  };

  const handleReportSuccess = async (options: {
    hidePost: boolean;
    hideUser: boolean;
  }) => {
    if (reportPostId === null) return;

    const reportedPost = posts.find((p) => p.postId === reportPostId);
    if (!reportedPost) return;

    if (options.hideUser) {
      await blockUser(reportedPost.userId);
      setIsBlocked(true);
      setProfile((prev) => (prev ? { ...prev, isFollowing: false } : null));
      toast.success("Đã chặn người dùng này.");
    }

    setPosts((prevPosts) => {
      let nextPosts = prevPosts;
      if (options.hidePost) {
        nextPosts = nextPosts.filter((p) => p.postId !== reportPostId);
      }
      if (options.hideUser) {
        nextPosts = nextPosts.filter((p) => p.userId !== reportedPost.userId);
      }
      return nextPosts;
    });
  };

  const handleToggleFollow = async () => {
    if (!profile || isOwnProfile || followLoading) return;

    const wasFollowing = profile.isFollowing;
    setFollowLoading(true);
    setProfile((prev) =>
      prev
        ? {
            ...prev,
            isFollowing: !wasFollowing,
            followers: Math.max(0, prev.followers + (wasFollowing ? -1 : 1)),
          }
        : prev,
    );

    const res = wasFollowing
      ? await unfollowUser(profile.userId)
      : await followUser(profile.userId);
    setFollowLoading(false);
    if (!res.ok) {
      setProfile((prev) =>
        prev
          ? {
              ...prev,
              isFollowing: wasFollowing,
              followers: Math.max(0, prev.followers + (wasFollowing ? 1 : -1)),
            }
          : prev,
      );
      toast.error(res.message ?? "Không thể cập nhật theo dõi.");
    }
  };

  const openFollowers = async () => {
    if (!profile) return;
    const res = await getFollowers(profile.userId, 0, 100);
    if (!res.ok || !res.data) {
      toast.error(res.message ?? "Không thể tải danh sách followers.");
      return;
    }
    setFollowersList(res.data.items ?? []);
    setShowFollowersModal(true);
  };

  const openFollowing = async () => {
    if (!profile) return;
    const res = await getFollowing(profile.userId, 0, 100);
    if (!res.ok || !res.data) {
      toast.error(res.message ?? "Không thể tải danh sách following.");
      return;
    }
    setFollowingList(res.data.items ?? []);
    setShowFollowingModal(true);
  };

  // Text profile save handler
  const handleSaveProfile = async () => {
    const res = await updateProfile({
      displayName: editDisplayName,
      bio: editBio,
    });

    if (res.ok) {
      toast.success("Cập nhật thông tin thành công!");
      setShowEditModal(false);
      loadData();
    } else {
      toast.error(res.message || "Cập nhật thất bại.");
    }
  };

  // Cover photo upload handler
  const handleCoverUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingCover(true);
    toast.loading("Đang tải ảnh bìa lên...", { id: "cover-upload" });

    const uploadRes = await uploadMedia(file);
    if (uploadRes.ok && uploadRes.data) {
      const updateRes = await updateProfile({ coverImageUrl: uploadRes.data });
      if (updateRes.ok) {
        toast.success("Đã thay đổi ảnh bìa thành công!", {
          id: "cover-upload",
        });
        loadData();
      } else {
        toast.error(updateRes.message || "Cập nhật hồ sơ thất bại.", {
          id: "cover-upload",
        });
      }
    } else {
      toast.error(uploadRes.message || "Tải ảnh lên thất bại.", {
        id: "cover-upload",
      });
    }
    setUploadingCover(false);
  };

  // Avatar select triggers adjust modal
  const handleAvatarSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setOriginalAvatarFile(file);

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === "string") {
        const img = new Image();
        img.src = reader.result;
        img.onload = () => {
          setPreviewAspect(img.width / img.height);
          setAvatarPreviewSrc(reader.result as string);
          setZoom(1);
          setOffset({ x: 0, y: 0 });
        };
      }
    };
    reader.readAsDataURL(file);
  };

  // Avatar Cropper Mouse/Touch Event Handlers
  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    setDragStart({ x: e.clientX - offset.x, y: e.clientY - offset.y });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    setOffset({
      x: e.clientX - dragStart.x,
      y: e.clientY - dragStart.y,
    });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 1) {
      setIsDragging(true);
      setDragStart({
        x: e.touches[0].clientX - offset.x,
        y: e.touches[0].clientY - offset.y,
      });
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isDragging || e.touches.length !== 1) return;
    setOffset({
      x: e.touches[0].clientX - dragStart.x,
      y: e.touches[0].clientY - dragStart.y,
    });
  };

  const handleDeleteAvatar = async () => {
    if (!window.confirm("Bạn có chắc muốn xóa ảnh đại diện hiện tại?")) return;

    setUploadingAvatar(true);
    toast.loading("Đang xóa ảnh đại diện...", { id: "avatar-upload" });

    const res = await updateProfile({
      avatarUrl: "DELETE",
      avatarPublicId: "DELETE",
    });

    setUploadingAvatar(false);
    if (res.ok) {
      toast.success("Đã xóa ảnh đại diện thành công!", { id: "avatar-upload" });
      loadData();
    } else {
      toast.error(res.message || "Xóa ảnh đại diện thất bại.", {
        id: "avatar-upload",
      });
    }
  };

  const handleDeleteCover = async () => {
    if (!window.confirm("Bạn có chắc muốn xóa ảnh bìa hiện tại?")) return;

    setUploadingCover(true);
    toast.loading("Đang xóa ảnh bìa...", { id: "cover-upload" });

    const res = await updateProfile({
      coverImageUrl: "DELETE",
      coverImagePublicId: "DELETE",
    });

    setUploadingCover(false);
    if (res.ok) {
      toast.success("Đã xóa ảnh bìa thành công!", { id: "cover-upload" });
      loadData();
    } else {
      toast.error(res.message || "Xóa ảnh bìa thất bại.", {
        id: "cover-upload",
      });
    }
  };

  const handleRecropCurrentAvatar = () => {
    if (!profile?.avatarUrl) return;
    // Re-crop from original if available, otherwise fallback to current avatar
    const sourceImage = profile.avatarPublicId || profile.avatarUrl;

    const img = new Image();
    img.src = sourceImage;
    img.crossOrigin = "anonymous";
    img.onload = () => {
      setPreviewAspect(img.width / img.height);
      setAvatarPreviewSrc(sourceImage);
      setOriginalAvatarFile(null);
      setZoom(1);
      setOffset({ x: 0, y: 0 });
    };
  };

  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.05 : 0.95;
    setZoom((prev) => {
      const next = prev * factor;
      return Math.max(1, Math.min(4, next));
    });
  };

  const handleAvatarCropConfirm = () => {
    if (!avatarPreviewSrc) return;

    setUploadingAvatar(true);
    toast.loading("Đang cắt và tải ảnh đại diện lên...", {
      id: "avatar-upload",
    });

    const img = new Image();
    img.src = avatarPreviewSrc;
    img.crossOrigin = "anonymous";
    img.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = 400;
      canvas.height = 400;
      const ctx = canvas.getContext("2d");

      if (ctx) {
        ctx.fillStyle = "#ffffff";
        ctx.fillRect(0, 0, 400, 400);

        // Circular clip for clean crop saving
        ctx.beginPath();
        ctx.arc(200, 200, 200, 0, Math.PI * 2);
        ctx.clip();

        // Calculate exact scale matches
        const imgAspect = img.width / img.height;
        let baseWidth = 300;
        let baseHeight = 300;
        if (imgAspect > 1) {
          baseWidth = 300 * imgAspect;
          baseHeight = 300;
        } else {
          baseWidth = 300;
          baseHeight = 300 / imgAspect;
        }

        const scaleFactor = 400 / 300;
        const sw = baseWidth * scaleFactor * zoom;
        const sh = baseHeight * scaleFactor * zoom;

        // Perfectly centered alignment
        const sx = 200 - sw / 2 + offset.x * scaleFactor;
        const sy = 200 - sh / 2 + offset.y * scaleFactor;

        ctx.drawImage(img, sx, sy, sw, sh);

        canvas.toBlob(async (blob) => {
          if (blob) {
            try {
              let originalUrl = profile?.avatarPublicId || null;

              // Upload new original file if one was selected
              if (originalAvatarFile) {
                const origRes = await uploadMedia(originalAvatarFile);
                if (origRes.ok && origRes.data) {
                  originalUrl = origRes.data;
                } else {
                  toast.error(
                    "Tải ảnh gốc lên thất bại. Đang thử lưu ảnh đã cắt...",
                    { id: "avatar-upload" },
                  );
                }
              }

              const croppedFile = new File([blob], "avatar.png", {
                type: "image/png",
              });
              const uploadRes = await uploadMedia(croppedFile);
              if (uploadRes.ok && uploadRes.data) {
                const updateRes = await updateProfile({
                  avatarUrl: uploadRes.data,
                  avatarPublicId: originalUrl || uploadRes.data,
                });
                if (updateRes.ok) {
                  toast.success("Thay đổi ảnh đại diện thành công!", {
                    id: "avatar-upload",
                  });
                  setAvatarPreviewSrc(null);
                  setOriginalAvatarFile(null);
                  loadData();
                } else {
                  toast.error(updateRes.message || "Cập nhật hồ sơ thất bại.", {
                    id: "avatar-upload",
                  });
                }
              } else {
                toast.error(uploadRes.message || "Tải lên ảnh thất bại.", {
                  id: "avatar-upload",
                });
              }
            } catch {
              toast.error("Lỗi khi tải ảnh lên.", { id: "avatar-upload" });
            }
          }
          setUploadingAvatar(false);
        }, "image/png");
      }
    };
  };

  if (loading) {
    return (
      <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
        <AppHeader />
        <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
          <AppSidebar active="profile" />
          <div className="flex justify-center py-24">
            <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
          </div>
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
        <AppHeader />
        <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
          <AppSidebar active="profile" />
          <div className="flex flex-col items-center justify-center py-24 text-gray-500 dark:text-neutral-500">
            <h2 className="text-2xl font-bold mb-2 text-gray-900 dark:text-white">
              Không tìm thấy người dùng
            </h2>
            <button
              onClick={() => navigate(-1)}
              className="text-blue-500 hover:underline"
            >
              Quay lại
            </button>
          </div>
        </div>
      </div>
    );
  }

  const isOwnProfile =
    !username ||
    (myProfile && profile && profile.username === myProfile.username);
  const coverUrl = profile.coverImageUrl;

  const baseWidth = previewAspect > 1 ? 300 * previewAspect : 300;
  const baseHeight = previewAspect > 1 ? 300 : 300 / previewAspect;

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />

      {/* 3-COLUMN GRID */}
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] xl:grid-cols-[280px_1fr_350px] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
        {/* LEFT COLUMN: SIDEBAR */}
        <AppSidebar active="profile" />

        {/* MIDDLE COLUMN: PROFILE HEADER & TIMELINE */}
        <div className="flex flex-col gap-6 min-w-0 pb-24 lg:pb-10">
          {/* PROFILE CARD: COVER & AVATAR & BASIC DETAILS */}
          <div className="bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-[0_4px_20px_rgba(0,0,0,0.03)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.4)] overflow-hidden">
            {/* COVER */}
            <div className="h-48 sm:h-64 bg-[radial-gradient(ellipse_at_top_right,_var(--tw-gradient-stops))] from-blue-100 via-blue-50 to-cyan-50 dark:from-blue-900/40 dark:via-[#1e1e1e] dark:to-[#121212] relative group overflow-hidden border-b border-slate-200/80 dark:border-[#2a2a2a]">
              {coverUrl && (
                <img
                  src={coverUrl}
                  alt="Cover"
                  onClick={() => setViewingImageUrl(coverUrl)}
                  className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105 cursor-pointer"
                  title="Nhấp để xem ảnh bìa đầy đủ"
                />
              )}
              {isOwnProfile && (
                <>
                  <input
                    type="file"
                    ref={coverInputRef}
                    onChange={handleCoverUpload}
                    accept="image/*"
                    className="hidden"
                  />
                  <div className="absolute right-4 bottom-4 flex items-center gap-2 z-20">
                    {coverUrl && (
                      <button
                        onClick={handleDeleteCover}
                        disabled={uploadingCover}
                        className="p-3 bg-red-650 hover:bg-red-750 rounded-full text-white flex items-center justify-center shadow-md cursor-pointer transition-all active:scale-95 duration-200"
                        title="Xóa ảnh bìa"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    )}
                    <button
                      onClick={() => coverInputRef.current?.click()}
                      disabled={uploadingCover}
                      className="p-3 bg-black/60 hover:bg-black/80 rounded-full text-white flex items-center justify-center shadow-md cursor-pointer transition-all active:scale-95 duration-200"
                      title="Thay đổi ảnh bìa"
                    >
                      {uploadingCover ? (
                        <Loader2 className="w-5 h-5 animate-spin" />
                      ) : (
                        <Camera className="w-5 h-5" />
                      )}
                    </button>
                  </div>
                </>
              )}
              <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent pointer-events-none" />
            </div>

            {/* AVATAR & BASIC METADATA */}
            <div className="px-6 pb-6 relative">
              <div className="flex justify-between items-end relative -mt-16 sm:-mt-20 mb-4">
                {/* Avatar */}
                <div
                  onClick={() => {
                    if (!isOwnProfile && profile.avatarUrl) {
                      setViewingImageUrl(
                        profile.avatarPublicId || profile.avatarUrl,
                      );
                    }
                  }}
                  className={`relative group/avatar w-32 h-32 sm:w-40 sm:h-40 rounded-full border-[8px] border-white dark:border-[#1e1e1e] overflow-hidden bg-slate-100 dark:bg-neutral-800 shadow-xl z-10 ${
                    !isOwnProfile && profile.avatarUrl
                      ? "cursor-pointer hover:opacity-90 transition-opacity"
                      : ""
                  }`}
                  title={
                    !isOwnProfile && profile.avatarUrl
                      ? "Nhấp để xem ảnh đại diện gốc đầy đủ"
                      : undefined
                  }
                >
                  <SafeAvatar src={profile.avatarUrl} alt={profile.username} />
                  {isOwnProfile && (
                    <>
                      <input
                        type="file"
                        ref={avatarInputRef}
                        onChange={handleAvatarSelect}
                        accept="image/*"
                        className="hidden"
                      />
                      <div className="absolute inset-0 bg-black/60 opacity-0 group-hover/avatar:opacity-100 transition-all duration-300 flex flex-col items-center justify-center text-white z-20">
                        <span className="text-[11px] font-bold uppercase tracking-wider mb-2 text-slate-300">
                          Ảnh đại diện
                        </span>
                        <div className="flex items-center gap-1.5">
                          {profile.avatarUrl && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setViewingImageUrl(
                                  profile.avatarPublicId || profile.avatarUrl,
                                );
                              }}
                              className="p-1.5 rounded-full bg-white/20 hover:bg-white/40 backdrop-blur-md transition-all hover:scale-115 border border-white/20 cursor-pointer"
                              title="Xem ảnh gốc đầy đủ"
                            >
                              <Eye className="w-3.5 h-3.5 text-white" />
                            </button>
                          )}
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              avatarInputRef.current?.click();
                            }}
                            className="p-1.5 rounded-full bg-white/20 hover:bg-white/40 backdrop-blur-md transition-all hover:scale-115 border border-white/20 cursor-pointer"
                            title="Tải ảnh mới lên"
                          >
                            <Camera className="w-3.5 h-3.5 text-white" />
                          </button>
                          {profile.avatarUrl && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleRecropCurrentAvatar();
                              }}
                              className="p-1.5 rounded-full bg-white/20 hover:bg-white/40 backdrop-blur-md transition-all hover:scale-115 border border-white/20 cursor-pointer"
                              title="Cắt lại ảnh gốc này"
                            >
                              <Crop className="w-3.5 h-3.5 text-white" />
                            </button>
                          )}
                          {profile.avatarUrl && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                handleDeleteAvatar();
                              }}
                              className="p-1.5 rounded-full bg-red-600/70 hover:bg-red-650 backdrop-blur-md transition-all hover:scale-115 border border-red-500/20 cursor-pointer"
                              title="Xóa ảnh đại diện"
                            >
                              <Trash2 className="w-3.5 h-3.5 text-white" />
                            </button>
                          )}
                        </div>
                      </div>
                    </>
                  )}
                </div>

                {/* Edit / Follow Action Button on the right */}
                <div className="shrink-0 pb-2">
                  {isOwnProfile ? (
                    <button
                      onClick={() => setShowEditModal(true)}
                      className="px-5 py-2 font-bold rounded-full border border-slate-200 dark:border-neutral-800 hover:bg-slate-100 dark:hover:bg-neutral-800 transition-all text-sm hover:border-slate-300 dark:hover:border-neutral-700 shadow-sm active:scale-95 text-slate-800 dark:text-white"
                    >
                      Chỉnh sửa hồ sơ
                    </button>
                  ) : (
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() =>
                          navigate(`${PATHS.CHAT}?userId=${profile.userId}`)
                        }
                        disabled={isBlocked}
                        className="px-5 py-2 font-bold rounded-full border border-blue-200 dark:border-blue-500/30 bg-blue-50/80 dark:bg-blue-500/10 text-blue-700 dark:text-blue-300 hover:bg-blue-100 dark:hover:bg-blue-500/20 transition-all text-sm active:scale-95 shadow-md disabled:opacity-50"
                      >
                        <MessageSquare className="mr-2 inline-block h-4 w-4 align-[-0.125em]" />
                        Nhắn tin
                      </button>
                      <button
                        onClick={handleToggleFollow}
                        disabled={followLoading || isBlocked}
                        className={`px-5 py-2 font-bold rounded-full transition-all text-sm active:scale-95 shadow-md disabled:opacity-50 ${
                          profile.isFollowing
                            ? "border border-slate-300 dark:border-neutral-700 hover:bg-red-50 hover:text-red-500 hover:border-red-200 text-slate-700 dark:text-neutral-300"
                            : "bg-slate-900 text-white dark:bg-white dark:text-black hover:bg-slate-800 dark:hover:bg-neutral-200"
                        }`}
                      >
                        {followLoading
                          ? "Đang xử lý..."
                          : profile.isFollowing
                            ? "Đang theo dõi"
                            : "Theo dõi"}
                      </button>
                      <button
                        onClick={handleToggleBlock}
                        disabled={blockLoading}
                        className={`px-5 py-2 font-bold rounded-full transition-all text-sm active:scale-95 shadow-md disabled:opacity-50 ${
                          isBlocked
                            ? "bg-red-650 hover:bg-red-750 text-white border border-red-650"
                            : "border border-slate-300 dark:border-neutral-700 hover:bg-red-50 hover:text-red-500 text-slate-700 dark:text-neutral-300"
                        }`}
                      >
                        {blockLoading
                          ? "Đang xử lý..."
                          : isBlocked
                            ? "Bỏ chặn"
                            : "Chặn"}
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Identity Header */}
              <div className="mt-2">
                <h2 className="font-extrabold text-2xl tracking-tight text-gray-900 dark:text-white leading-tight">
                  {profile.displayName || profile.username}
                </h2>
                <p className="text-sm text-gray-500 dark:text-neutral-400 font-medium">
                  @{profile.username}
                </p>

                {/* Meta stats for mobile */}
                <div className="flex items-center gap-4 mt-3 text-xs text-gray-500 dark:text-neutral-500 font-semibold lg:hidden">
                  <span>{profile.postCount} bài đăng</span>
                  <button onClick={openFollowing} className="hover:underline">
                    <strong className="text-gray-900 dark:text-white font-extrabold">
                      {profile.following}
                    </strong>{" "}
                    đang theo dõi
                  </button>
                  <button onClick={openFollowers} className="hover:underline">
                    <strong className="text-gray-900 dark:text-white font-extrabold">
                      {profile.followers}
                    </strong>{" "}
                    người theo dõi
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* POSTS TIMELINE */}
          <div className="flex flex-col gap-4">
            {isBlocked ? (
              <div className="text-center py-20 bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] p-8 shadow-sm">
                <UserX className="w-12 h-12 text-slate-400 mx-auto mb-3" />
                <h3 className="text-lg font-bold text-gray-900 dark:text-white">
                  Bạn đã chặn người dùng này
                </h3>
                <p className="text-sm text-slate-500 mt-1">
                  Hủy chặn để có thể xem bài viết và tương tác với họ.
                </p>
              </div>
            ) : postsLoading ? (
              <div className="flex justify-center py-16">
                <Loader2 className="w-8 h-8 animate-spin text-blue-500" />
              </div>
            ) : !posts || posts.length === 0 ? (
              <div className="text-center py-20 text-gray-500 dark:text-neutral-500 bg-white dark:bg-[#1e1e1e] rounded-2xl border border-slate-200/80 dark:border-[#2a2a2a] shadow-[0_4px_20px_rgba(0,0,0,0.03)]">
                <p className="text-lg font-bold text-gray-900 dark:text-white">
                  Chưa có bài viết
                </p>
                <p className="text-sm">
                  Khi người dùng này đăng bài, chúng sẽ hiển thị ở đây.
                </p>
              </div>
            ) : (
              posts.map((post) => (
                <ProfilePost
                  key={post.postId}
                  post={post}
                  displayName={profile?.displayName || post.username}
                  handleReact={handleReact}
                  isReacting={reactingPostIds.has(post.postId)}
                  currentUserId={myProfile?.userId}
                  onEdit={setEditingPost}
                  onDelete={handleDeletePost}
                  isBookmarked={bookmarkedPostIds.has(post.postId)}
                  onToggleBookmark={handleToggleBookmark}
                  onReport={setReportPostId}
                  onShare={setSharingPost}
                />
              ))
            )}
          </div>
        </div>

        {/* RIGHT COLUMN: ABOUT / INTRO INFO SIDEBAR */}
        <aside className="hidden lg:flex flex-col gap-6 sticky top-24 h-fit">
          <div className="bg-white dark:bg-[#1e1e1e] rounded-2xl p-5 border border-slate-200/80 dark:border-[#2a2a2a] shadow-[0_4px_20px_rgba(0,0,0,0.03)] dark:shadow-[0_4px_25px_rgba(0,0,0,0.4)]">
            {/* ABOUT */}
            <div className="flex items-center gap-2 mb-3">
              <h3 className="font-bold text-lg text-gray-900 dark:text-white">
                Giới thiệu
              </h3>
            </div>

            {profile.bio ? (
              <p className="text-slate-700 dark:text-neutral-300 text-sm mb-5 leading-relaxed whitespace-pre-line break-words font-medium">
                {profile.bio}
              </p>
            ) : (
              <div className="flex items-start gap-2 text-slate-400 dark:text-neutral-500 text-xs mb-5 italic font-medium">
                <Edit3 className="w-4 h-4 mt-0.5 shrink-0" />
                <p className="leading-relaxed">Chưa có mô tả bản thân.</p>
              </div>
            )}

            {/* METADATA JOINED DATE */}
            <div className="flex items-center gap-2 text-xs text-gray-500 dark:text-neutral-400 pt-4 border-t border-slate-100 dark:border-neutral-800/80">
              <CalendarDays className="w-4 h-4 text-gray-400" />
              <span>Đã tham gia SocialPulse</span>
            </div>

            {/* STATS */}
            <div className="grid grid-cols-2 gap-3 mt-5 pt-4 border-t border-slate-100 dark:border-neutral-800/60">
              <button
                onClick={openFollowing}
                className="cursor-pointer hover:bg-slate-50 dark:hover:bg-neutral-800/30 p-2.5 rounded-xl transition-colors flex flex-col items-center text-center border border-slate-100 dark:border-neutral-800"
              >
                <span className="font-extrabold text-xl text-gray-900 dark:text-white tracking-tight">
                  {profile.following}
                </span>
                <span className="text-[9px] font-bold text-gray-400 dark:text-neutral-500 uppercase tracking-wider mt-1">
                  Đang theo dõi
                </span>
              </button>
              <button
                onClick={openFollowers}
                className="cursor-pointer hover:bg-slate-50 dark:hover:bg-neutral-800/30 p-2.5 rounded-xl transition-colors flex flex-col items-center text-center border border-slate-100 dark:border-neutral-800"
              >
                <span className="font-extrabold text-xl text-gray-900 dark:text-white tracking-tight">
                  {profile.followers}
                </span>
                <span className="text-[9px] font-bold text-gray-400 dark:text-neutral-500 uppercase tracking-wider mt-1">
                  Người theo dõi
                </span>
              </button>
            </div>
          </div>
        </aside>
      </div>

      {/* TEXT PROFILE EDIT MODAL */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-neutral-900 w-full max-w-lg rounded-2xl overflow-hidden shadow-2xl border border-slate-200 dark:border-neutral-800 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center px-6 py-4 border-b border-slate-100 dark:border-neutral-800">
              <h3 className="font-bold text-lg text-gray-900 dark:text-white">
                Chỉnh sửa hồ sơ
              </h3>
              <button
                onClick={() => setShowEditModal(false)}
                className="p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-800 text-gray-400 dark:text-neutral-500 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 flex flex-col gap-5">
              <div>
                <label className="block text-xs font-extrabold text-gray-500 dark:text-neutral-400 uppercase tracking-wider mb-2">
                  Tên hiển thị
                </label>
                <input
                  type="text"
                  value={editDisplayName}
                  onChange={(e) => setEditDisplayName(e.target.value)}
                  className="w-full bg-slate-50 dark:bg-neutral-950 border border-slate-200 dark:border-neutral-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 rounded-xl py-3 px-4 outline-none text-sm transition-all dark:text-white"
                  placeholder="Nhập tên hiển thị mới của bạn"
                />
              </div>

              <div>
                <label className="block text-xs font-extrabold text-gray-500 dark:text-neutral-400 uppercase tracking-wider mb-2">
                  Mô tả bản thân (Bio)
                </label>
                <textarea
                  value={editBio}
                  rows={4}
                  onChange={(e) => setEditBio(e.target.value)}
                  className="w-full bg-slate-50 dark:bg-neutral-950 border border-slate-200 dark:border-neutral-800 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 rounded-xl py-3 px-4 outline-none text-sm transition-all dark:text-white resize-none leading-relaxed"
                  placeholder="Hãy chia sẻ đôi nét về bản thân của bạn với mọi người..."
                />
              </div>
            </div>

            <div className="flex justify-end items-center gap-3 px-6 py-4 bg-slate-50 dark:bg-neutral-950 border-t border-slate-100 dark:border-neutral-800">
              <button
                onClick={() => setShowEditModal(false)}
                className="px-5 py-2.5 rounded-xl border border-slate-200 dark:border-neutral-800 text-sm font-semibold hover:bg-slate-100 dark:hover:bg-neutral-900 transition-colors"
              >
                Hủy bỏ
              </button>
              <button
                onClick={handleSaveProfile}
                className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-sm font-semibold transition-colors shadow-lg shadow-blue-500/20 active:scale-95"
              >
                Lưu thay đổi
              </button>
            </div>
          </div>
        </div>
      )}

      <CreatePostModal
        isOpen={!!editingPost}
        mode="edit"
        initialPost={editingPost}
        onClose={() => setEditingPost(null)}
        currentUserAvatar={myProfile?.avatarUrl || undefined}
        currentUsername={myProfile?.displayName || myProfile?.username}
        onPostUpdated={handlePostUpdated}
      />
      <CreatePostModal
        isOpen={!!sharingPost}
        mode="create"
        parentPostId={sharingPost?.postId}
        parentPostAuthor={sharingPost?.username}
        parentPostContent={sharingPost?.content}
        onClose={() => setSharingPost(null)}
        currentUserAvatar={myProfile?.avatarUrl || undefined}
        currentUsername={myProfile?.displayName || myProfile?.username}
        onPostCreated={() => {
          setSharingPost(null);
          loadData();
        }}
      />
      <UserListModal
        isOpen={showFollowersModal}
        title="Người theo dõi"
        users={followersList}
        onClose={() => setShowFollowersModal(false)}
      />
      <UserListModal
        isOpen={showFollowingModal}
        title="Đang theo dõi"
        users={followingList}
        onClose={() => setShowFollowingModal(false)}
      />
      <ReportModal
        isOpen={reportPostId !== null}
        targetType="POST"
        targetId={reportPostId}
        title="bài viết"
        onClose={() => setReportPostId(null)}
        onReportSuccess={handleReportSuccess}
      />

      {/* PREMIUM CIRCULAR AVATAR ADJUSTMENT & CROP MODAL */}
      {avatarPreviewSrc && (
        <div className="fixed inset-0 bg-black/85 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-neutral-900 w-full max-w-md rounded-2xl overflow-hidden shadow-2xl border border-slate-200 dark:border-neutral-800 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex justify-between items-center px-6 py-4 border-b border-slate-100 dark:border-neutral-800">
              <h3 className="font-bold text-lg text-gray-900 dark:text-white">
                Căn chỉnh ảnh đại diện
              </h3>
              <button
                onClick={() => setAvatarPreviewSrc(null)}
                className="p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-800 text-gray-400 dark:text-neutral-500 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-8 flex flex-col items-center">
              <div
                className="w-[300px] h-[300px] bg-slate-900 relative overflow-hidden rounded-lg cursor-move select-none touch-none shadow-inner"
                onMouseDown={handleMouseDown}
                onMouseMove={handleMouseMove}
                onMouseUp={handleMouseUp}
                onMouseLeave={handleMouseUp}
                onTouchStart={handleTouchStart}
                onTouchMove={handleTouchMove}
                onTouchEnd={handleMouseUp}
                onWheel={handleWheel}
              >
                <img
                  ref={cropperImgRef}
                  src={avatarPreviewSrc}
                  alt="Cropper Preview"
                  className="max-w-none absolute pointer-events-none select-none"
                  style={{
                    transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`,
                    transformOrigin: "center center",
                    left: "50%",
                    top: "50%",
                    width: `${baseWidth}px`,
                    height: `${baseHeight}px`,
                    marginLeft: `${-baseWidth / 2}px`,
                    marginTop: `${-baseHeight / 2}px`,
                  }}
                />

                <svg
                  className="absolute inset-0 pointer-events-none w-full h-full"
                  viewBox="0 0 300 300"
                >
                  <defs>
                    <mask id="circle-mask">
                      <rect x="0" y="0" width="300" height="300" fill="white" />
                      <circle cx="150" cy="150" r="145" fill="black" />
                    </mask>
                  </defs>
                  <rect
                    x="0"
                    y="0"
                    width="300"
                    height="300"
                    fill="black"
                    opacity="0.65"
                    mask="url(#circle-mask)"
                  />
                  <circle
                    cx="150"
                    cy="150"
                    r="145"
                    stroke="#3b82f6"
                    strokeWidth="3"
                    strokeDasharray="5,5"
                    fill="none"
                  />
                </svg>
              </div>

              <div className="w-full mt-6 flex items-center justify-center gap-2.5 text-xs font-semibold text-slate-500 dark:text-neutral-400 bg-slate-50 dark:bg-neutral-950 py-3.5 px-4 rounded-xl border border-slate-200/50 dark:border-neutral-850">
                <MousePointerClick className="w-4 h-4 text-blue-500 animate-bounce shrink-0" />
                <span>
                  Lăn chuột hoặc dùng pad (2 ngón) để thu nhỏ / phóng to
                </span>
              </div>

              <p className="text-xs text-gray-500 dark:text-neutral-400 mt-4 text-center leading-relaxed">
                * Nhấp giữ kéo chuột/ngón tay để di chuyển góc ảnh phù hợp với
                vòng tròn tiêu điểm nét.
              </p>
            </div>

            <div className="flex justify-end items-center gap-3 px-6 py-4 bg-slate-50 dark:bg-neutral-950 border-t border-slate-100 dark:border-neutral-800">
              <button
                onClick={() => setAvatarPreviewSrc(null)}
                className="px-5 py-2.5 rounded-xl border border-slate-200 dark:border-neutral-800 text-sm font-semibold hover:bg-slate-100 dark:hover:bg-neutral-900 transition-colors"
                disabled={uploadingAvatar}
              >
                Hủy bỏ
              </button>
              <button
                onClick={handleAvatarCropConfirm}
                disabled={uploadingAvatar}
                className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-sm font-semibold transition-colors shadow-lg shadow-blue-500/20 active:scale-95 flex items-center gap-2"
              >
                {uploadingAvatar ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  "Xác nhận & Tải lên"
                )}
              </button>
            </div>
          </div>
        </div>
      )}
      {/* LIGHTBOX FOR FULL VIEWING AVATAR / COVER */}
      {viewingImageUrl && (
        <div
          className="fixed inset-0 bg-black/90 backdrop-blur-md z-50 flex items-center justify-center p-4 cursor-zoom-out animate-in fade-in duration-200"
          onClick={() => setViewingImageUrl(null)}
        >
          <div
            className="relative max-w-4xl max-h-[90vh] overflow-hidden rounded-2xl border border-neutral-850 bg-neutral-900/50 shadow-2xl animate-in zoom-in-95 duration-200"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setViewingImageUrl(null)}
              className="absolute right-4 top-4 p-2 rounded-full bg-black/60 hover:bg-black/80 text-white transition-colors z-10 cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
            <img
              src={viewingImageUrl}
              alt="Full view"
              className="max-w-full max-h-[85vh] object-contain mx-auto"
            />
          </div>
        </div>
      )}
      <BottomNavBar active="profile" />
    </div>
  );
}
