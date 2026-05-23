/**
 * PostMedia component - Displays post images and videos
 *
 * Supports single or multiple media items with responsive layouts.
 * Handles both images and videos with fullscreen lightbox navigation.
 */

import { useState, useEffect } from "react";
import { ChevronLeft, ChevronRight, X, Play } from "lucide-react";
import { isVideo } from "@/lib/mediaUtils";

interface PostMediaProps {
  urls: string[];
  variant?: "feed" | "profile";
}

export function PostMedia({ urls }: PostMediaProps) {
  const [activeIdx, setActiveIdx] = useState<number | null>(null);

  useEffect(() => {
    if (activeIdx === null) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setActiveIdx(null);
      } else if (e.key === "ArrowLeft") {
        setActiveIdx((prev) => (prev !== null && prev > 0 ? prev - 1 : prev));
      } else if (e.key === "ArrowRight") {
        setActiveIdx((prev) => (prev !== null && prev < urls.length - 1 ? prev + 1 : prev));
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [activeIdx, urls.length]);

  if (!urls || urls.length === 0) return null;

  // Helper to render media element inside the feed/profile grid collage
  const renderGridMedia = (url: string, idx: number) => {
    if (isVideo(url)) {
      return (
        <div className="relative w-full h-full group">
          <video
            src={url}
            className="w-full h-full object-cover block"
            muted
            playsInline
            controls={false}
          />
          <div className="absolute inset-0 bg-black/25 flex items-center justify-center group-hover:bg-black/35 transition-colors">
            <div className="w-10 h-10 rounded-full bg-white/90 shadow-md flex items-center justify-center group-hover:scale-110 transition-transform">
              <Play className="w-4 h-4 text-slate-800 fill-current translate-x-0.5" />
            </div>
          </div>
        </div>
      );
    }
    return (
      <img
        src={url}
        alt={`post-media-${idx}`}
        className="w-full h-full object-cover block hover:scale-[1.02] transition-transform duration-300"
        loading="lazy"
      />
    );
  };

  // Lightbox rendering helper
  const renderLightbox = () => {
    if (activeIdx === null) return null;
    const activeUrl = urls[activeIdx];
    const activeIsVideo = isVideo(activeUrl);

    return (
      <div
        className="fixed inset-0 bg-black/95 backdrop-blur-md z-[150] flex items-center justify-center p-4 cursor-zoom-out animate-in fade-in duration-200"
        onClick={() => setActiveIdx(null)}
      >
        {/* Close Button */}
        <button
          onClick={() => setActiveIdx(null)}
          title="Đóng"
          className="absolute right-6 top-6 p-2.5 rounded-full bg-black/60 hover:bg-black/80 hover:scale-110 text-white transition-all z-[160] cursor-pointer"
        >
          <X className="w-6 h-6" />
        </button>

        {/* Index indicator */}
        {urls.length > 1 && (
          <div className="absolute top-6 left-6 text-white bg-black/60 px-4 py-2 rounded-full font-bold text-sm select-none backdrop-blur-sm z-[160]">
            {activeIdx + 1} / {urls.length}
          </div>
        )}

        {/* Left Arrow */}
        {urls.length > 1 && activeIdx > 0 && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setActiveIdx((prev) => (prev !== null ? prev - 1 : null));
            }}
            title="Trước"
            className="absolute left-6 p-3 rounded-full bg-black/50 hover:bg-black/80 text-white hover:scale-110 transition-all z-[160] cursor-pointer"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
        )}

        {/* Right Arrow */}
        {urls.length > 1 && activeIdx < urls.length - 1 && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              setActiveIdx((prev) => (prev !== null ? prev + 1 : null));
            }}
            title="Sau"
            className="absolute right-6 p-3 rounded-full bg-black/50 hover:bg-black/80 text-white hover:scale-110 transition-all z-[160] cursor-pointer"
          >
            <ChevronRight className="w-6 h-6" />
          </button>
        )}

        {/* Media Player/Viewer Container */}
        <div
          className="relative max-w-5xl max-h-[85vh] overflow-hidden flex items-center justify-center p-2"
          onClick={(e) => e.stopPropagation()}
        >
          {activeIsVideo ? (
            <video
              src={activeUrl}
              controls
              autoPlay
              className="max-w-full max-h-[85vh] object-contain rounded-xl shadow-2xl animate-in zoom-in-95 duration-200"
            />
          ) : (
            <img
              src={activeUrl}
              alt="Full view"
              className="max-w-full max-h-[85vh] object-contain rounded-xl shadow-2xl animate-in zoom-in-95 duration-200"
            />
          )}
        </div>
      </div>
    );
  };

  // Single media item: original size, no crop, no max-height limits
  if (urls.length === 1) {
    const url = urls[0];
    return (
      <>
        <div
          onClick={() => setActiveIdx(0)}
          className="overflow-hidden border border-slate-200/80 dark:border-neutral-800 rounded-2xl bg-slate-50 dark:bg-neutral-900 w-full mb-3 cursor-zoom-in"
        >
          {isVideo(url) ? (
            <div className="relative w-full h-auto max-h-[650px] overflow-hidden group">
              <video
                src={url}
                className="w-full h-auto object-contain block max-h-[650px] mx-auto"
              />
              <div className="absolute inset-0 bg-black/20 group-hover:bg-black/35 flex items-center justify-center transition-colors">
                <div className="w-14 h-14 rounded-full bg-white/90 shadow-lg flex items-center justify-center hover:scale-110 transition-all">
                  <Play className="w-6 h-6 text-slate-800 fill-current translate-x-0.5" />
                </div>
              </div>
            </div>
          ) : (
            <img
              src={url}
              alt="post"
              className="w-full h-auto object-contain block mx-auto"
            />
          )}
        </div>
        {renderLightbox()}
      </>
    );
  }

  // Multiple media items: modern collage grids
  const containerClass = `w-full overflow-hidden border border-slate-200/80 dark:border-neutral-800 rounded-2xl bg-slate-50 dark:bg-neutral-900 mb-3 aspect-[16/10] max-h-[420px] cursor-zoom-in`;

  // 2 images: 2 columns
  if (urls.length === 2) {
    return (
      <>
        <div className={`grid grid-cols-2 gap-1.5 ${containerClass}`}>
          {urls.map((url, idx) => (
            <div key={idx} onClick={() => setActiveIdx(idx)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(url, idx)}
            </div>
          ))}
        </div>
        {renderLightbox()}
      </>
    );
  }

  // 3 images: 1 large left, 2 stacked right
  if (urls.length === 3) {
    return (
      <>
        <div className={`grid grid-cols-3 gap-1.5 ${containerClass}`}>
          <div onClick={() => setActiveIdx(0)} className="col-span-2 relative overflow-hidden w-full h-full">
            {renderGridMedia(urls[0], 0)}
          </div>
          <div className="col-span-1 grid grid-rows-2 gap-1.5 h-full">
            <div onClick={() => setActiveIdx(1)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[1], 1)}
            </div>
            <div onClick={() => setActiveIdx(2)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[2], 2)}
            </div>
          </div>
        </div>
        {renderLightbox()}
      </>
    );
  }

  // 4 images: 2x2 grid
  if (urls.length === 4) {
    return (
      <>
        <div className={`grid grid-cols-2 grid-rows-2 gap-1.5 ${containerClass}`}>
          {urls.map((url, idx) => (
            <div key={idx} onClick={() => setActiveIdx(idx)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(url, idx)}
            </div>
          ))}
        </div>
        {renderLightbox()}
      </>
    );
  }

  // 5 images: top row 2, bottom row 3
  if (urls.length === 5) {
    return (
      <>
        <div className={`flex flex-col gap-1.5 ${containerClass}`}>
          <div className="grid grid-cols-2 gap-1.5 h-[50%]">
            <div onClick={() => setActiveIdx(0)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[0], 0)}
            </div>
            <div onClick={() => setActiveIdx(1)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[1], 1)}
            </div>
          </div>
          <div className="grid grid-cols-3 gap-1.5 h-[50%]">
            <div onClick={() => setActiveIdx(2)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[2], 2)}
            </div>
            <div onClick={() => setActiveIdx(3)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[3], 3)}
            </div>
            <div onClick={() => setActiveIdx(4)} className="relative overflow-hidden w-full h-full">
              {renderGridMedia(urls[4], 4)}
            </div>
          </div>
        </div>
        {renderLightbox()}
      </>
    );
  }

  // 6 or more images: 2x3 grid with +N overlay on the 6th image
  const remainingCount = urls.length - 6;
  return (
    <>
      <div className={`grid grid-cols-3 grid-rows-2 gap-1.5 ${containerClass}`}>
        {urls.slice(0, 6).map((url, idx) => (
          <div key={idx} onClick={() => setActiveIdx(idx)} className="relative overflow-hidden w-full h-full">
            {renderGridMedia(url, idx)}
            {idx === 5 && remainingCount > 0 && (
              <div className="absolute inset-0 bg-black/60 backdrop-blur-[2px] flex items-center justify-center text-white font-bold text-xl">
                +{remainingCount}
              </div>
            )}
          </div>
        ))}
      </div>
      {renderLightbox()}
    </>
  );
}
