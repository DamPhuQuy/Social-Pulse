/**
 * PostMedia component - Displays post images and videos
 *
 * Supports single or multiple media items with responsive layouts.
 * Handles both images and videos with appropriate rendering.
 */

import { isVideo } from "@/lib/mediaUtils";

interface PostMediaProps {
  urls: string[];
  variant?: "feed" | "profile";
}

export function PostMedia({ urls, variant = "feed" }: PostMediaProps) {
  if (!urls || urls.length === 0) return null;

  const isFeedVariant = variant === "feed";

  // Single media item
  if (urls.length === 1) {
    const url = urls[0];
    return (
      <div
        className={`overflow-hidden border border-slate-200 dark:border-neutral-800 ${
          isFeedVariant
            ? "rounded-2xl bg-slate-100 dark:bg-neutral-900 max-h-[500px] flex items-center justify-center"
            : "rounded-xl mb-3 bg-black"
        }`}
      >
        {isVideo(url) ? (
          <video
            src={url}
            controls
            className={`w-full ${
              isFeedVariant
                ? "h-full object-contain max-h-[500px]"
                : "h-auto max-h-[500px]"
            }`}
          />
        ) : (
          <img
            src={url}
            alt="post"
            className={`w-full ${
              isFeedVariant
                ? "h-full object-cover max-h-[500px]"
                : "h-auto object-cover max-h-[500px]"
            }`}
          />
        )}
      </div>
    );
  }

  // Multiple media items
  return (
    <div
      className={`grid gap-2 ${
        isFeedVariant
          ? "grid-cols-2 rounded-2xl overflow-hidden bg-slate-100 dark:bg-neutral-900 border border-slate-200 dark:border-neutral-800"
          : `${urls.length === 2 ? "grid-cols-2" : "grid-cols-2"} mb-3`
      }`}
    >
      {urls.map((url, idx) => (
        <div
          key={idx}
          className={`overflow-hidden ${
            isFeedVariant
              ? "aspect-video relative bg-gray-100 dark:bg-neutral-800"
              : `rounded-xl border border-slate-200 dark:border-neutral-800 bg-black ${
                  urls.length === 3 && idx === 0 ? "col-span-2" : ""
                }`
          }`}
        >
          {isVideo(url) ? (
            <video src={url} controls className="w-full h-48 object-cover" />
          ) : (
            <img
              src={url}
              alt={`post-media-${idx}`}
              className="w-full h-48 object-cover"
            />
          )}
        </div>
      ))}
    </div>
  );
}
