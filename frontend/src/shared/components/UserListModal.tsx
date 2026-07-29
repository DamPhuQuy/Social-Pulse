import { useState, useEffect } from "react";
import { X, Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { SafeAvatar } from "@/shared/components/ui/SafeAvatar";
import type { UserSummary } from "@/features/social-relations/infrastructure/api/followService";

interface UserListModalProps {
  isOpen: boolean;
  title: string;
  users: UserSummary[];
  onClose: () => void;
}

export default function UserListModal({ isOpen, title, users, onClose }: UserListModalProps) {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    if (!isOpen) {
      setSearchQuery("");
    }
  }, [isOpen]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "";
    }
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  const filteredUsers = users.filter((user) =>
    user.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-neutral-800 dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-neutral-800">
          <h2 className="font-bold text-slate-900 dark:text-white">{title}</h2>
          <button onClick={onClose} title="Đóng" className="rounded-full p-2 text-slate-400 hover:bg-slate-100 dark:hover:bg-neutral-800">
            <X className="h-4 w-4" />
          </button>
        </div>
        
        {/* Search Box */}
        <div className="p-3 border-b border-slate-100 dark:border-neutral-800">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400 dark:text-neutral-500" />
            <input
              type="text"
              placeholder="Tìm kiếm theo username..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full rounded-xl bg-slate-50 pl-9 pr-8 py-2 text-sm outline-none border border-transparent focus:border-blue-500 focus:bg-white transition-all dark:bg-neutral-950 dark:text-white dark:focus:bg-neutral-950"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery("")}
                title="Xóa tìm kiếm"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-neutral-300"
              >
                <X className="h-3 w-3" />
              </button>
            )}
          </div>
        </div>

        <div className="max-h-[60vh] overflow-y-auto p-4">
          {filteredUsers.length === 0 ? (
            <div className="py-10 text-center text-sm text-slate-500 dark:text-neutral-500">
              {searchQuery ? "Không tìm thấy người dùng phù hợp." : "Chưa có dữ liệu."}
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {filteredUsers.map((user) => (
                <button
                  key={`${user.id}-${user.username}`}
                  onClick={() => {
                    onClose();
                    navigate(`/profile/${user.username}`);
                  }}
                  className="flex items-center gap-3 rounded-2xl px-3 py-2 text-left hover:bg-slate-50 dark:hover:bg-neutral-800 w-full"
                >
                  <div className="h-10 w-10 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800 shrink-0">
                    <SafeAvatar src={user.avatarUrl} alt={user.username} />
                  </div>
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-slate-900 dark:text-white">@{user.username}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
