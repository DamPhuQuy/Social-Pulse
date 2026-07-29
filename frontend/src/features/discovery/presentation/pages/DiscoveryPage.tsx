import { useEffect, useState } from "react";
import { Compass, Hash, Loader2, Search, User } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { toast } from "sonner";
import AppHeader from "@/shared/components/AppHeader";
import AppSidebar from "@/shared/components/AppSidebar";
import BottomNavBar from "@/shared/components/BottomNavBar";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import {
  getPostsByHashtag,
  getPostsByMention,
  getTrendingHashtags,
  searchPosts,
  searchUsers,
  type SearchUserResponse,
  type TrendingHashtagResponse,
} from "@/features/discovery/infrastructure/api/discoveryService";
import type { UserPost } from "@/features/profiles/infrastructure/api/userService";

type SearchMode = "posts" | "users";

export default function DiscoveryPage() {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState(() => {
    return params.get("q") ?? sessionStorage.getItem("discovery_input") ?? "";
  });
  const [mode, setMode] = useState<SearchMode>((params.get("mode") as SearchMode) || "posts");
  const [posts, setPosts] = useState<UserPost[]>([]);
  const [users, setUsers] = useState<SearchUserResponse[]>([]);
  const [trending, setTrending] = useState<TrendingHashtagResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getTrendingHashtags().then((res) => {
      if (res.ok && res.data) {
        setTrending(res.data);
      }
    });
  }, []);

  useEffect(() => {
    const nextQuery = params.get("q") ?? "";
    const nextMode = (params.get("mode") as SearchMode) || "posts";
    const nextType = (params.get("type") as "search" | "hashtag" | "mention") || "search";

    if (params.get("q") !== null) {
      setQuery(nextQuery);
      sessionStorage.setItem("discovery_input", nextQuery);
    } else {
      const savedInput = sessionStorage.getItem("discovery_input");
      if (savedInput) {
        setQuery(savedInput);
      } else {
        setQuery("");
      }
    }
    setMode(nextMode);

    let mounted = true;
    const run = async () => {
      setLoading(true);
      let postRes;
      if (nextMode === "users" && nextQuery.trim()) {
        const userRes = await searchUsers(nextQuery.trim());
        if (!mounted) return;
        if (userRes.ok && userRes.data) setUsers(userRes.data.items ?? []);
        else if (userRes.message) toast.error(userRes.message);
        setPosts([]);
        setLoading(false);
        return;
      } else if (nextType === "hashtag" && nextQuery) {
        postRes = await getPostsByHashtag(nextQuery.replace(/^#/, ""));
      } else if (nextType === "mention" && nextQuery) {
        postRes = await getPostsByMention(nextQuery.replace(/^@/, ""));
      } else if (nextQuery.trim()) {
        postRes = await searchPosts(nextQuery.trim());
      } else {
        const trendingRes = await getTrendingHashtags();
        if (!mounted) return;
        if (trendingRes.ok && trendingRes.data) setTrending(trendingRes.data);
        setUsers([]);
        setPosts([]);
        setLoading(false);
        return;
      }

      if (!mounted) return;
      if (postRes?.ok && postRes.data) {
        setPosts(postRes.data.items ?? []);
      } else if (postRes?.message) {
        toast.error(postRes.message);
      }
      setUsers([]);
      setLoading(false);
    };

    run();
    return () => {
      mounted = false;
    };
  }, [params]);

  const submitSearch = (nextMode = mode) => {
    const trimmed = query.trim();
    if (!trimmed) {
      setParams({ mode: nextMode });
      return;
    }
    const type = trimmed.startsWith("#") ? "hashtag" : trimmed.startsWith("@") ? "mention" : "search";
    const normalized = type === "search" ? trimmed : trimmed.slice(1);
    setParams({ q: normalized, mode: nextMode, type });
  };

  return (
    <div className="bg-[#f3f4f6] dark:bg-[#121212] min-h-screen font-sans text-slate-800 dark:text-[#e4e6eb] transition-colors duration-300">
      <AppHeader />
      <div className="w-full grid grid-cols-1 lg:grid-cols-[260px_1fr] xl:grid-cols-[280px_1fr] gap-6 lg:gap-8 pt-20 lg:pt-24 px-4 sm:px-6 lg:px-10">
        <AppSidebar active="discovery" />

        <div className="flex min-w-0 flex-col gap-6 pb-24 lg:pb-10">

        <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
          <div className="flex flex-col gap-4 md:flex-row md:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
              <input
                value={query}
                onChange={(event) => {
                  const val = event.target.value;
                  setQuery(val);
                  sessionStorage.setItem("discovery_input", val);
                }}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    submitSearch();
                  }
                }}
                placeholder="Tìm bài viết, người dùng, hashtag hoặc @username"
                className="w-full rounded-full border border-slate-200 bg-slate-50 py-3 pl-11 pr-4 text-sm outline-none focus:border-slate-400 dark:border-neutral-800 dark:bg-neutral-950 dark:text-white"
              />
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => {
                  setMode("posts");
                  submitSearch("posts");
                }}
                className={`rounded-full px-4 py-2 text-sm font-semibold ${mode === "posts" ? "bg-slate-900 text-white dark:bg-white dark:text-slate-900" : "bg-slate-100 text-slate-600 dark:bg-neutral-900 dark:text-neutral-300"}`}
              >
                Bài viết
              </button>
              <button
                onClick={() => {
                  setMode("users");
                  submitSearch("users");
                }}
                className={`rounded-full px-4 py-2 text-sm font-semibold ${mode === "users" ? "bg-slate-900 text-white dark:bg-white dark:text-slate-900" : "bg-slate-100 text-slate-600 dark:bg-neutral-900 dark:text-neutral-300"}`}
              >
                Người dùng
              </button>
            </div>
          </div>
        </section>

          <div className="grid gap-6 grid-cols-1 lg:grid-cols-[1fr_280px]">
          <section className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
            {loading ? (
              <div className="flex justify-center py-16">
                <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
              </div>
            ) : mode === "users" ? (
              <div className="flex flex-col gap-3">
                {users.length === 0 ? (
                  <EmptyState label={query.trim() ? "Không tìm thấy người dùng phù hợp." : "Hãy nhập từ khóa để tìm kiếm người dùng."} />
                ) : (
                  users.map((item) => (
                    <button
                      key={item.id}
                      onClick={() => navigate(`/profile/${item.username}`)}
                      className="flex items-center gap-3 rounded-2xl border border-slate-200/80 p-4 text-left hover:bg-slate-50 dark:border-neutral-800 dark:hover:bg-neutral-900"
                    >
                      <div className="h-12 w-12 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
                        <SafeAvatar src={item.avatarUrl} alt={item.username} />
                      </div>
                      <div className="min-w-0">
                        <p className="truncate font-bold text-slate-900 dark:text-white">{item.displayName || item.username}</p>
                        <p className="truncate text-sm text-slate-500 dark:text-neutral-400">@{item.username}</p>
                      </div>
                    </button>
                  ))
                )}
              </div>
            ) : (
              <div className="flex flex-col gap-4">
                {posts.length === 0 ? (
                  <EmptyState label={query.trim() ? "Không tìm thấy bài viết phù hợp." : "Hãy nhập từ khóa để tìm kiếm bài viết."} />
                ) : (
                  posts.map((post) => (
                    <button
                      key={post.postId}
                      onClick={() => navigate(`/posts/${post.postId}`)}
                      className="rounded-2xl border border-slate-200/80 p-5 text-left hover:bg-slate-50 dark:border-neutral-800 dark:hover:bg-neutral-900"
                    >
                      <div className="mb-3 flex items-center gap-3">
                        <div className="h-10 w-10 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
                          <SafeAvatar src={post.userAvatar} alt={post.username} />
                        </div>
                        <div className="min-w-0">
                          <p className="truncate font-semibold text-slate-900 dark:text-white">{post.username}</p>
                          <p className="text-xs text-slate-500 dark:text-neutral-400">
                            {post.upvoteCount} pulse · {post.cmtCount} bình luận
                          </p>
                        </div>
                      </div>
                      <p className="line-clamp-4 whitespace-pre-line text-sm leading-6 text-slate-700 dark:text-neutral-300">{post.content}</p>
                      {post.topicSlugs?.length > 0 && (
                        <div className="mt-3 flex flex-wrap gap-2">
                          {post.topicSlugs.map((topic) => (
                            <span key={topic} className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-500 dark:bg-neutral-800 dark:text-neutral-400">
                              #{topic}
                            </span>
                          ))}
                        </div>
                      )}
                    </button>
                  ))
                )}
              </div>
            )}
          </section>

          <aside className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm dark:border-[#2a2a2a] dark:bg-[#1e1e1e]">
            <div className="mb-4 flex items-center gap-2">
              <Hash className="h-4 w-4 text-blue-500" />
              <h2 className="font-bold">Hashtag thịnh hành</h2>
            </div>
            <div className="flex flex-col gap-2">
              {trending.map((item) => (
                <button
                  key={item.hashtag}
                  onClick={() => setParams({ q: item.hashtag, mode: "posts", type: "hashtag" })}
                  className="rounded-xl border border-slate-200/80 px-4 py-3 text-left hover:bg-slate-50 dark:border-neutral-800 dark:hover:bg-neutral-900"
                >
                  <p className="font-semibold text-slate-900 dark:text-white">#{item.hashtag}</p>
                  <p className="text-xs text-slate-500 dark:text-neutral-400">{item.count} bài viết</p>
                </button>
              ))}
            </div>
            <div className="mt-5 rounded-xl bg-slate-50 p-4 dark:bg-neutral-900">
              <div className="mb-2 flex items-center gap-2 text-sm font-semibold">
                <User className="h-4 w-4" />
                Gợi ý tìm mention
              </div>
              <button
                onClick={() => setParams({ q: "socialpulse", mode: "posts", type: "mention" })}
                className="text-sm text-blue-600 hover:underline dark:text-blue-400"
              >
                Xem các bài nhắc tới `@socialpulse`
              </button>
            </div>
          </aside>
          </div>
        </div>
      </div>
      <BottomNavBar active="discovery" />
    </div>
  );
}

function EmptyState({ label }: { label: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 text-center">
      <Compass className="mb-3 h-10 w-10 text-slate-300 dark:text-neutral-700" />
      <p className="text-sm font-medium text-slate-500 dark:text-neutral-500">{label}</p>
    </div>
  );
}
