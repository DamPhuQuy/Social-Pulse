/**
 * Number formatting utilities
 */

/**
 * Format a number with K/M/B suffixes
 * @param count - The number to format
 * @returns Formatted string (e.g., "1.2K", "1.5M", "2.3B")
 *
 * @example
 * formatCount(1234) // "1.2K"
 * formatCount(1500000) // "1.5M"
 * formatCount(42) // "42"
 */
export function formatCount(count: number): string {
  if (count < 1000) {
    return count.toString();
  }

  if (count < 1000000) {
    return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'K';
  }

  if (count < 1000000000) {
    return (count / 1000000).toFixed(1).replace(/\.0$/, '') + 'M';
  }

  return (count / 1000000000).toFixed(1).replace(/\.0$/, '') + 'B';
}

/**
 * Format bytes to human-readable size
 * @param bytes - The number of bytes
 * @returns Formatted string (e.g., "1.2 KB", "1.5 MB")
 *
 * @example
 * formatBytes(1024) // "1.0 KB"
 * formatBytes(1536000) // "1.5 MB"
 */
export function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 Bytes';

  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}
