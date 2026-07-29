import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { toast } from "sonner";
import { Edit3, Loader2, X } from "lucide-react";

import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import ReportModal from "@/shared/components/ReportModal";
import UserListModal from "@/shared/components/UserListModal";
import { PostCard } from "@/features/feed/presentation/components/PostCard";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";

import { PATHS } from "@/shared/constants/paths";
import { useAuth } from "@/shared/hooks/useAuth";
import { usePostActions } from "@/shared/hooks/usePostActions";

import { checkIsBlocked } from "@/features/social-relations/infrastructure/api/blockService";
import { getBookmarks } from "@/features/bookmarks/infrastructure/api/bookmarkService";
import {
  followUser,
  getFollowers,
  getFollowing,
  unfollowUser,
  type UserSummary,
} from "@/features/social-relations/infrastructure/api/followService";
import {
  getMyProfile,
  getUserPosts,
  getUserProfile,
  updateProfile,
  type UserProfile,
} from "@/features/profiles/infrastructure/api/userService";
import { uploadMedia, type FeedItem } from "@/features/feed/infrastructure/api/postService";

export default function ProfilePage() {
  const { username: paramUsername } = useParams<{ username?: string }>();
  const navigate = useNavigate();
  const { accessToken } = useAuth();

  const [myProfile, setMyProfile] = useState<UserProfile | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [posts, setPosts] = useState<FeedItem[]>([]);
  const [bookmarkedPostIds, setBookmarkedPostIds] = useState<Set<number>>(() => new Set());
  const [isFollowing, setIsFollowing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [followingAction, setFollowingAction] = useState(false);

  // Modals state
  const [showEditModal, setShowEditModal] = useState(false);
  const [showFollowersModal, setShowFollowersModal] = useState(false);
  const [showFollowingModal, setShowFollowingModal] = useState(false);
  const [userList, setUserList] = useState<UserSummary[]>([]);
  const [userListTitle, setUserListTitle] = useState("");

  // Edit profile form
  const [editForm, setEditForm] = useState({ displayName: "", bio: "" });
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [savingProfile, setSavingProfile] = useState(false);

  const {
    reactingPostIds,
    setEditingPost,
    reportPostId,
    setReportPostId,
    setSharingPost,
    handleReact,
    handleToggleBookmark,
    handleDeletePost,
    handleBlockUser,
  } = usePostActions<FeedItem>(posts, setPosts, bookmarkedPostIds, setBookmarkedPostIds);

  const isOwnProfile = !paramUsername || (myProfile && myProfile.username === paramUsername);

  const fetchProfileData = useCallback(async () => {
    setLoading(true);
    try {
      if (accessToken) {
        const myRes = await getMyProfile();
        if (myRes.ok && myRes.data) {
          setMyProfile(myRes.data);
        }
        const bRes = await getBookmarks(0, 100).catch(() => ({ ok: false, data: { items: [] } }));
        if (bRes.ok && bRes.data) {
          setBookmarkedPostIds(new Set((bRes.data.items ?? []).map((i: any) => i.postId)));
        }
      }

      const targetUsername = paramUsername || myProfile?.username;
      if (!targetUsername && !accessToken) {
        setLoading(false);
        return;
      }

      let pData: UserProfile | null = null;
      if (!paramUsername || (myProfile && myProfile.username === paramUsername)) {
        const res = await getMyProfile();
        if (res.ok && res.data) pData = res.data;
      } else {
        const res = await getUserProfile(paramUsername);
        if (res.ok && res.data) pData = res.data;
      }

      setProfile(pData);
      if (pData) {
        setIsFollowing(!!pData.isFollowing);
        setEditForm({ displayName: pData.displayName || "", bio: pData.bio || "" });

        if (accessToken && !isOwnProfile) {
          await checkIsBlocked(pData.userId);
        }

        const postsRes = await getUserPosts(pData.userId, 0, 50);
        if (postsRes.ok && postsRes.data) {
          const mappedItems: FeedItem[] = (postsRes.data.items || []).map((p: any) => ({
            postId: p.postId,
            parentPostId: p.parentPostId || null,
            type: p.type || "ORIGINAL",
            content: p.content || "",
            imageUrl: p.imageUrl || null,
            topicSlugs: p.topicSlugs || [],
            userId: p.userId,
            username: p.username || pData?.username || "",
            userAvatar: p.userAvatar || pData?.avatarUrl || null,
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
          setPosts(mappedItems);
        }
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [accessToken, isOwnProfile, myProfile?.username, paramUsername]);

  useEffect(() => {
    fetchProfileData();
  }, [fetchProfileData]);

  const handleFollowToggle = async () => {
    if (!accessToken || !profile) {
      toast.error("Vui lòng đăng nhập.");
      navigate(PATHS.LOGIN);
      return;
    }
    setFollowingAction(true);
    try {
      if (isFollowing) {
        const res = await unfollowUser(profile.userId);
        if (res.ok) {
          setIsFollowing(false);
          setProfile((prev) => prev ? { ...prev, followers: Math.max(0, prev.followers - 1) } : null);
          toast.success(`Đã bỏ theo dõi @${profile.username}`);
        }
      } else {
        const res = await followUser(profile.userId);
        if (res.ok) {
          setIsFollowing(true);
          setProfile((prev) => prev ? { ...prev, followers: prev.followers + 1 } : null);
          toast.success(`Đã theo dõi @${profile.username}`);
        }
      }
    } finally {
      setFollowingAction(false);
    }
  };

  const handleSaveProfile = async () => {
    if (!profile) return;
    setSavingProfile(true);
    try {
      let avatarUrl = profile.avatarUrl || undefined;
      if (avatarFile) {
        const upRes = await uploadMedia(avatarFile);
        if (upRes.ok && upRes.data) {
          avatarUrl = typeof upRes.data === "string" ? upRes.data : (upRes.data as any).url;
        }
      }
      const res = await updateProfile({
        displayName: editForm.displayName,
        bio: editForm.bio,
        avatarUrl,
      });
      if (res.ok && res.data) {
        setProfile(res.data);
        setMyProfile(res.data);
        setShowEditModal(false);
        toast.success("Cập nhật trang cá nhân thành công.");
      } else {
        toast.error(res.message || "Cập nhật thất bại.");
      }
    } catch (e) {
      console.error(e);
      toast.error("Lỗi khi lưu thông tin.");
    } finally {
      setSavingProfile(false);
    }
  };

  const openFollowers = async () => {
    if (!profile) return;
    setUserListTitle("Người theo dõi");
    setShowFollowersModal(true);
    const res = await getFollowers(profile.userId);
    if (res.ok && res.data) setUserList(res.data.items || []);
  };

  const openFollowing = async () => {
    if (!profile) return;
    setUserListTitle("Đang theo dõi");
    setShowFollowingModal(true);
    const res = await getFollowing(profile.userId);
    if (res.ok && res.data) setUserList(res.data.items || []);
  };

  return (
    <div className="bg-[#ffffff] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr_320px] gap-6 lg:gap-8 pt-6 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="profile" />

        <main className="flex flex-col gap-6 pb-24 lg:pb-10 min-w-0">
          {loading ? (
            <div className="flex justify-center py-20">
              <Loader2 className="w-8 h-8 animate-spin text-[#0064e0]" />
            </div>
          ) : !profile ? (
            <div className="text-center py-20 bg-white dark:bg-[#1e1e1e] rounded-3xl border border-slate-200/80 p-8">
              <p className="text-lg font-bold text-slate-700">Không tìm thấy người dùng</p>
            </div>
          ) : (
            <>
              {/* PROFILE CARD HEADER (DESIGN.md style) */}
              <div className="bg-white dark:bg-[#1e1e1e] border border-slate-200/80 dark:border-[#2a2a2a] rounded-3xl p-6 shadow-sm">
                <div className="flex flex-col sm:flex-row items-center sm:items-start gap-6">
                  <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-[#0064e0] shrink-0">
                    <SafeAvatar src={profile.avatarUrl} alt={profile.username} />
                  </div>

                  <div className="flex-1 text-center sm:text-left">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                      <div>
                        <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white">
                          {profile.displayName || profile.username}
                        </h1>
                        <p className="text-sm font-semibold text-slate-400">@{profile.username}</p>
                      </div>

                      {isOwnProfile ? (
                        <button
                          onClick={() => setShowEditModal(true)}
                          className="px-5 py-2 rounded-full bg-[#000000] text-white dark:bg-white dark:text-slate-900 text-sm font-bold hover:opacity-90 transition-opacity"
                        >
                          <Edit3 className="w-4 h-4 inline mr-2" /> Chỉnh sửa trang cá nhân
                        </button>
                      ) : (
                        <button
                          onClick={handleFollowToggle}
                          disabled={followingAction}
                          className={`px-6 py-2 rounded-full text-sm font-bold transition-all ${
                            isFollowing
                              ? "bg-slate-100 text-slate-700 dark:bg-neutral-800 dark:text-neutral-200"
                              : "bg-[#0064e0] text-white hover:bg-[#0457cb]"
                          }`}
                        >
                          {isFollowing ? "Đã theo dõi" : "+ Theo dõi"}
                        </button>
                      )}
                    </div>

                    {profile.bio && (
                      <p className="mt-3 text-sm text-slate-700 dark:text-neutral-300 whitespace-pre-wrap">
                        {profile.bio}
                      </p>
                    )}

                    <div className="flex items-center gap-6 mt-4 pt-4 border-t border-slate-100 dark:border-neutral-800 justify-center sm:justify-start">
                      <div onClick={openFollowers} className="cursor-pointer hover:underline">
                        <span className="font-extrabold text-slate-900 dark:text-white mr-1">{profile.followers}</span>
                        <span className="text-xs text-slate-400 font-semibold">Người theo dõi</span>
                      </div>

                      <div onClick={openFollowing} className="cursor-pointer hover:underline">
                        <span className="font-extrabold text-slate-900 dark:text-white mr-1">{profile.following}</span>
                        <span className="text-xs text-slate-400 font-semibold">Đang theo dõi</span>
                      </div>

                      <div>
                        <span className="font-extrabold text-slate-900 dark:text-white mr-1">{posts.length}</span>
                        <span className="text-xs text-slate-400 font-semibold">Bài viết</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* USER POSTS LIST */}
              <div className="flex flex-col gap-4">
                <h2 className="text-lg font-bold text-slate-900 dark:text-white px-2">Bài viết</h2>
                {posts.length === 0 ? (
                  <div className="text-center py-16 bg-white dark:bg-[#1e1e1e] rounded-3xl border border-slate-200/80 p-8">
                    <p className="text-slate-400 font-semibold">Chưa có bài viết nào.</p>
                  </div>
                ) : (
                  posts.map((post) => (
                    <PostCard
                      key={post.postId}
                      post={post}
                      currentUserId={myProfile?.userId}
                      isBookmarked={bookmarkedPostIds.has(post.postId)}
                      isReacting={reactingPostIds.has(post.postId)}
                      onReact={handleReact}
                      onToggleBookmark={handleToggleBookmark}
                      onEdit={setEditingPost}
                      onDelete={handleDeletePost}
                      onReport={setReportPostId}
                      onBlockUser={handleBlockUser}
                      onShare={setSharingPost}
                    />
                  ))
                )}
              </div>
            </>
          )}
        </main>
      </div>

      {/* EDIT PROFILE MODAL */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-[#1e1e1e] rounded-3xl w-full max-w-md p-6 shadow-2xl animate-in zoom-in-95 duration-150">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-lg text-slate-900 dark:text-white">Chỉnh sửa thông tin</h3>
              <button onClick={() => setShowEditModal(false)} className="p-1 rounded-full hover:bg-slate-100 dark:hover:bg-neutral-800">
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>

            <div className="flex flex-col gap-4">
              <div>
                <label className="text-xs font-bold text-slate-500 uppercase">Tên hiển thị</label>
                <input
                  type="text"
                  value={editForm.displayName}
                  onChange={(e) => setEditForm({ ...editForm, displayName: e.target.value })}
                  className="w-full mt-1 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-neutral-800 text-sm focus:outline-none focus:border-[#0064e0]"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase">Tiểu sử (Bio)</label>
                <textarea
                  value={editForm.bio}
                  onChange={(e) => setEditForm({ ...editForm, bio: e.target.value })}
                  rows={3}
                  className="w-full mt-1 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-neutral-800 text-sm focus:outline-none focus:border-[#0064e0]"
                />
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase">Ảnh đại diện</label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      setAvatarFile(e.target.files[0]);
                    }
                  }}
                  className="mt-1 text-sm text-slate-500"
                />
              </div>

              <div className="flex justify-end gap-3 mt-4">
                <button
                  onClick={() => setShowEditModal(false)}
                  className="px-5 py-2 rounded-full border border-slate-200 text-sm font-bold"
                >
                  Hủy
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={savingProfile}
                  className="px-5 py-2 rounded-full bg-[#0064e0] text-white text-sm font-bold hover:bg-[#0457cb]"
                >
                  {savingProfile ? <Loader2 className="w-4 h-4 animate-spin" /> : "Lưu thay đổi"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* FOLLOWERS / FOLLOWING MODALS */}
      <UserListModal
        isOpen={showFollowersModal}
        title={userListTitle}
        users={userList}
        onClose={() => setShowFollowersModal(false)}
      />
      <UserListModal
        isOpen={showFollowingModal}
        title={userListTitle}
        users={userList}
        onClose={() => setShowFollowingModal(false)}
      />

      <ReportModal
        isOpen={reportPostId !== null}
        targetType="POST"
        targetId={reportPostId}
        title="bài viết"
        onClose={() => setReportPostId(null)}
        onReportSuccess={fetchProfileData}
      />
      <BottomNavBar active="profile" />
    </div>
  );
}
