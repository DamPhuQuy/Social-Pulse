/**
 * Date and time formatting utilities
 */

/**
 * Format a date string as relative time (e.g., "2 hours ago", "3 days ago")
 * @param dateStr - ISO date string or date string to format
 * @returns Formatted relative time string in Vietnamese
 *
 * @example
 * timeAgo("2024-01-15T10:30:00Z") // "2 giờ trước" (if current time is 2 hours later)
 * timeAgo("2024-01-14T10:30:00Z") // "1 ngày trước" (if current time is 1 day later)
 */
export function timeAgo(dateStr: string): string {
  if (!dateStr) return "";

  // Parse date - handle both ISO format and non-ISO format
  const parsedDate = !dateStr.endsWith("Z") && !dateStr.includes("+")
    ? new Date(dateStr.includes("T") ? dateStr + "Z" : dateStr.replace(" ", "T") + "Z")
    : new Date(dateStr);

  const diff = (Date.now() - parsedDate.getTime()) / 1000; // difference in seconds

  if (diff < 60) return `Vừa xong`;
  if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
  if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
  return `${Math.floor(diff / 86400)} ngày trước`;
}

/**
 * Format a date to a readable string
 * @param date - Date object or ISO string
 * @returns Formatted date string (e.g., "15 Jan 2024")
 *
 * @example
 * formatDate(new Date("2024-01-15")) // "15 Jan 2024"
 * formatDate("2024-01-15T10:30:00Z") // "15 Jan 2024"
 */
export function formatDate(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;

  const day = d.getDate();
  const month = d.toLocaleString('en-US', { month: 'short' });
  const year = d.getFullYear();

  return `${day} ${month} ${year}`;
}

/**
 * Format a date with time to a readable string
 * @param date - Date object or ISO string
 * @returns Formatted date and time string (e.g., "15 Jan 2024, 10:30 AM")
 *
 * @example
 * formatDateTime(new Date("2024-01-15T10:30:00Z")) // "15 Jan 2024, 10:30 AM"
 * formatDateTime("2024-01-15T10:30:00Z") // "15 Jan 2024, 10:30 AM"
 */
export function formatDateTime(date: Date | string): string {
  const d = typeof date === 'string' ? new Date(date) : date;

  const dateStr = formatDate(d);
  const timeStr = d.toLocaleString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  });

  return `${dateStr}, ${timeStr}`;
}
