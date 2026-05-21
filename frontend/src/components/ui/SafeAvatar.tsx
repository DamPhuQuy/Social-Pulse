/**
 * SafeAvatar component - Displays user avatar with fallback
 *
 * Shows a default user icon SVG when no avatar image is provided.
 * Used throughout the app for user profile pictures.
 */

interface SafeAvatarProps {
  src?: string | null;
  alt?: string;
  className?: string;
}

export function SafeAvatar({
  src,
  alt,
  className = "w-full h-full object-cover",
}: SafeAvatarProps) {
  if (!src) {
    return (
      <div className="w-full h-full bg-slate-200 dark:bg-neutral-800 flex items-center justify-center text-slate-400 dark:text-neutral-500">
        <svg className="w-1/2 h-1/2" fill="currentColor" viewBox="0 0 24 24">
          <path d="M24 20.993V24H0v-2.996A14.977 14.977 0 0 1 12.004 15c4.904 0 9.26 2.354 11.996 5.993zM16.002 8.999a4 4 0 1 1-8 0 4 4 0 0 1 8 0z" />
        </svg>
      </div>
    );
  }
  return <img src={src} alt={alt || "Avatar"} className={className} />;
}
