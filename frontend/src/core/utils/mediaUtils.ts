/**
 * Media type detection utilities
 */

/**
 * Check if a URL points to a video file
 * @param url - The URL to check
 * @returns true if the URL is a video, false otherwise
 *
 * @example
 * isVideo("https://example.com/video.mp4") // true
 * isVideo("https://example.com/image.jpg") // false
 * isVideo("https://res.cloudinary.com/video/upload/v1/video.mp4") // true
 */
export function isVideo(url: string): boolean {
  if (!url) return false;
  return url.match(/\.(mp4|webm|ogg|mov)$/i) !== null || url.includes("video/upload");
}

/**
 * Check if a URL points to an image file
 * @param url - The URL to check
 * @returns true if the URL is an image, false otherwise
 *
 * @example
 * isImage("https://example.com/image.jpg") // true
 * isImage("https://example.com/video.mp4") // false
 */
export function isImage(url: string): boolean {
  if (!url) return false;
  return url.match(/\.(jpg|jpeg|png|gif|webp|svg|bmp)$/i) !== null || url.includes("image/upload");
}

/**
 * Get the media type of a URL
 * @param url - The URL to check
 * @returns 'video', 'image', or 'unknown'
 *
 * @example
 * getMediaType("https://example.com/video.mp4") // "video"
 * getMediaType("https://example.com/image.jpg") // "image"
 * getMediaType("https://example.com/file.pdf") // "unknown"
 */
export function getMediaType(url: string): 'video' | 'image' | 'unknown' {
  if (isVideo(url)) return 'video';
  if (isImage(url)) return 'image';
  return 'unknown';
}
