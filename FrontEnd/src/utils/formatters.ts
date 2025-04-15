/**
 * Utility functions for formatting data in the application
 */

/**
 * Format file size from bytes to human-readable format
 * @param bytes File size in bytes
 * @returns Formatted file size string (e.g., "1.5 MB")
 */
export const formatFileSize = (bytes: number | null | undefined): string => {
  // Ensure bytes is a valid number, default to 0 if not
  const numBytes = Number(bytes); 
  if (isNaN(numBytes) || numBytes === null || numBytes < 0) {
    return '0 B'; // Return 0 B for invalid or non-numeric input
  }

  if (numBytes < 1024) return numBytes + ' B';
  if (numBytes < 1048576) return (numBytes / 1024).toFixed(1) + ' KB';
  if (numBytes < 1073741824) return (numBytes / 1048576).toFixed(1) + ' MB';
  return (numBytes / 1073741824).toFixed(1) + ' GB';
};

/**
 * Format date string to localized date
 * @param dateString ISO date string or any valid date string
 * @returns Formatted date string using locale format
 */
export const formatDate = (dateString: string): string => {
  if (!dateString) return 'N/A';
  
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  } catch (error) {
    console.error('Error formatting date:', error);
    return 'Invalid date';
  }
};

/**
 * Format a number with thousands separators
 * @param num Number to format
 * @returns Formatted number string
 */
export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat().format(num);
};

/**
 * Format a timestamp to relative time (e.g., "2 hours ago")
 * @param dateString ISO date string or any valid date string
 * @returns Relative time string
 */
export const formatRelativeTime = (dateString: string): string => {
  if (!dateString) return 'N/A';
  
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.round(diffMs / 1000);
    const diffMin = Math.round(diffSec / 60);
    const diffHour = Math.round(diffMin / 60);
    const diffDay = Math.round(diffHour / 24);
    
    if (diffSec < 60) return `${diffSec} seconds ago`;
    if (diffMin < 60) return `${diffMin} minutes ago`;
    if (diffHour < 24) return `${diffHour} hours ago`;
    if (diffDay < 30) return `${diffDay} days ago`;
    
    return formatDate(dateString);
  } catch (error) {
    console.error('Error formatting relative time:', error);
    return 'Invalid date';
  }
};
