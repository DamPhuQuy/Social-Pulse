import React from "react";
import { Compass, Clock, Bookmark, Hash, ChevronDown, Search } from "lucide-react";
import type { PostTopic } from "@/features/feed/infrastructure/api/postService";

export type FeedMode = "discover" | "following" | "bookmarks" | "topic";

interface FeedTabsProps {
  feedMode: FeedMode;
  onSelectMode: (mode: FeedMode) => void;
  selectedTopic: string | null;
  onSelectTopic: (slug: string | null) => void;
  topics: PostTopic[];
  followedTopicSlugs: Set<string>;
  onToggleFollowTopic: (slug: string) => void;
  isDropdownOpen: boolean;
  setIsDropdownOpen: (open: boolean) => void;
  dropdownSearch: string;
  setDropdownSearch: (search: string) => void;
  dropdownRef: React.RefObject<HTMLDivElement | null>;
}

export const FeedTabs: React.FC<FeedTabsProps> = ({
  feedMode,
  onSelectMode,
  selectedTopic,
  onSelectTopic,
  topics,
  followedTopicSlugs,
  onToggleFollowTopic,
  isDropdownOpen,
  setIsDropdownOpen,
  dropdownSearch,
  setDropdownSearch,
  dropdownRef,
}) => {
  const filteredTopics = topics.filter(
    (t) =>
      t.label.toLowerCase().includes(dropdownSearch.toLowerCase()) ||
      t.slug.toLowerCase().includes(dropdownSearch.toLowerCase())
  );

  return (
    <div className="flex gap-2 items-center relative z-30 flex-wrap">
      {/* Tab: Khám phá */}
      <button
        onClick={() => {
          onSelectMode("discover");
          onSelectTopic(null);
        }}
        className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold flex items-center gap-2 transition-all ${
          feedMode === "discover"
            ? "bg-slate-900 text-white dark:bg-white dark:text-zinc-900 shadow-sm"
            : "bg-white text-slate-600 dark:bg-zinc-900 dark:text-zinc-300 border border-slate-200/80 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-800"
        }`}
      >
        <Compass className="w-4 h-4" /> Khám phá
      </button>

      {/* Tab: Mới nhất */}
      <button
        onClick={() => {
          onSelectMode("following");
          onSelectTopic(null);
        }}
        className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold flex items-center gap-2 transition-all ${
          feedMode === "following"
            ? "bg-slate-900 text-white dark:bg-white dark:text-zinc-900 shadow-sm"
            : "bg-white text-slate-600 dark:bg-zinc-900 dark:text-zinc-300 border border-slate-200/80 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-800"
        }`}
      >
        <Clock className="w-4 h-4" /> Mới nhất
      </button>

      {/* Tab: Đã lưu */}
      <button
        onClick={() => {
          onSelectMode("bookmarks");
          onSelectTopic(null);
        }}
        className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold flex items-center gap-2 transition-all ${
          feedMode === "bookmarks"
            ? "bg-blue-600 text-white shadow-sm"
            : "bg-white text-slate-600 dark:bg-zinc-900 dark:text-zinc-300 border border-slate-200/80 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-800"
        }`}
      >
        <Bookmark className="w-4 h-4" /> Đã lưu
      </button>

      {/* Dropdown: Chủ đề */}
      <div ref={dropdownRef} className="relative">
        <button
          onClick={() => setIsDropdownOpen(!isDropdownOpen)}
          className={`px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold flex items-center gap-2 transition-all ${
            feedMode === "topic"
              ? "bg-blue-600 text-white shadow-sm"
              : "bg-white text-slate-600 dark:bg-zinc-900 dark:text-zinc-300 border border-slate-200/80 dark:border-zinc-800 hover:bg-slate-50 dark:hover:bg-zinc-800"
          }`}
        >
          <Hash className="w-4 h-4" />
          <span>{selectedTopic ? topics.find((t) => t.slug === selectedTopic)?.label || selectedTopic : "Chủ đề..."}</span>
          <ChevronDown className={`w-3.5 h-3.5 transition-transform duration-200 ${isDropdownOpen ? "rotate-180" : ""}`} />
        </button>

        {isDropdownOpen && (
          <div className="absolute left-0 top-full mt-2 w-72 bg-white dark:bg-zinc-900 border border-slate-200/80 dark:border-zinc-800 rounded-2xl shadow-2xl z-50 p-3 flex flex-col gap-2 animate-in fade-in duration-150">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400" />
              <input
                type="text"
                value={dropdownSearch}
                onChange={(e) => setDropdownSearch(e.target.value)}
                placeholder="Tìm công nghệ / chủ đề..."
                className="w-full bg-slate-50 dark:bg-zinc-800 border border-slate-200 dark:border-zinc-700 rounded-lg py-1.5 pl-8 pr-3 text-xs focus:outline-none focus:border-blue-500"
                autoFocus
              />
            </div>

            <div className="max-h-56 overflow-y-auto pr-1 flex flex-col gap-1">
              {filteredTopics.map((topic) => {
                const isFollowing = followedTopicSlugs.has(topic.slug);
                return (
                  <div
                    key={topic.slug}
                    className="flex items-center justify-between px-2.5 py-1.5 rounded-lg hover:bg-slate-50 dark:hover:bg-zinc-800 transition-colors"
                  >
                    <span
                      onClick={() => {
                        onSelectTopic(topic.slug);
                        onSelectMode("topic");
                        setIsDropdownOpen(false);
                      }}
                      className="text-xs font-medium text-slate-800 dark:text-zinc-200 cursor-pointer flex-1 truncate"
                    >
                      #{topic.label}
                    </span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleFollowTopic(topic.slug);
                      }}
                      className={`px-2 py-0.5 rounded-md text-[10px] font-semibold transition-all ${
                        isFollowing
                          ? "bg-slate-100 text-slate-600 dark:bg-zinc-700 dark:text-zinc-300"
                          : "bg-blue-600 text-white hover:bg-blue-700"
                      }`}
                    >
                      {isFollowing ? "Đang theo dõi" : "+ Theo dõi"}
                    </button>
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
