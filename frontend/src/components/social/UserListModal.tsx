import { X } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { SafeAvatar } from "@/pages/ProfilePage";
import type { UserSummary } from "@/services/social/followService";

interface UserListModalProps {
  isOpen: boolean;
  title: string;
  users: UserSummary[];
  onClose: () => void;
}

export default function UserListModal({ isOpen, title, users, onClose }: UserListModalProps) {
  const navigate = useNavigate();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white shadow-2xl dark:border-neutral-800 dark:bg-neutral-900">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-neutral-800">
          <h2 className="font-bold text-slate-900 dark:text-white">{title}</h2>
          <button onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-100 dark:hover:bg-neutral-800">
            <X className="h-4 w-4" />
          </button>
        </div>
        <div className="max-h-[60vh] overflow-y-auto p-4">
          {users.length === 0 ? (
            <div className="py-10 text-center text-sm text-slate-500 dark:text-neutral-500">Chưa có dữ liệu.</div>
          ) : (
            <div className="flex flex-col gap-2">
              {users.map((user) => (
                <button
                  key={`${user.id}-${user.username}`}
                  onClick={() => {
                    onClose();
                    navigate(`/profile/${user.username}`);
                  }}
                  className="flex items-center gap-3 rounded-2xl px-3 py-2 text-left hover:bg-slate-50 dark:hover:bg-neutral-800"
                >
                  <div className="h-10 w-10 overflow-hidden rounded-full border border-slate-200 dark:border-neutral-800">
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
