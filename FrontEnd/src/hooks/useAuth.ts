import { useState, useEffect, useCallback } from 'react';

interface User {
  id: string; // Ensure this matches the structure stored in localStorage
  username: string;
  email: string;
  roles: string[];
  // Add other fields if stored by authService.login
}

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  // Start with loading true until we check storage
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check storage only on the client side
    if (typeof window !== 'undefined') {
      try {
        const storedUser = localStorage.getItem('user');
        const token = localStorage.getItem('token'); // Also check if token exists

        if (storedUser && token) {
          const parsedUser: User = JSON.parse(storedUser);
          // Optional: Add basic validation for the parsed user object
          if (parsedUser && parsedUser.id && parsedUser.username) {
            setUser(parsedUser);
          } else {
            console.warn('Invalid user data found in localStorage.'); // Keep this warning
            localStorage.removeItem('user'); // Clean up invalid data
            localStorage.removeItem('token'); // Also remove token if user data is bad
            setUser(null);
          }
        } else {
          // No stored user or no token
          setUser(null);
          // If there's a user but no token, clear the user for consistency
          if (storedUser && !token) {
              localStorage.removeItem('user');
          }
          // DO NOT clear the token if the user object is missing.
        }
      } catch (error) {
        console.error('Error reading auth state from localStorage:', error); // Keep this error log
        // Clear potentially corrupted data
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        setUser(null);
      } finally {
        // Finished checking storage
        setLoading(false);
      }
    } else {
        // If on server, default to not logged in and not loading
        setLoading(false);
    }
    // This effect should run only once on mount to get initial state
  }, []);

  // Optionally, expose functions to manually update/clear user if needed elsewhere,
  // but for now, rely on page refresh after login/logout.
  return { user, loading };
}
