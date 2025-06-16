'use client'; // Add this directive

import { useState, useEffect, useCallback, useContext, createContext } from 'react';
import { authService } from '@/services/auth.service';

interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
  // Add other relevant fields if needed (e.g., enabled, accountLocked etc. if returned by login)
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  loading: boolean;
  login: (credentials: any) => Promise<void>; // Adjust credentials type as needed
  logout: () => void;
}

// Create a context with a default undefined value to check if provider is used
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// AuthProvider component to wrap the application
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true); // Start loading until initial check is done

  // Check localStorage on initial mount (client-side only)
  useEffect(() => {
    if (typeof window === 'undefined') {
      setLoading(false);
      return;
    }

    try {
      const storedToken = localStorage.getItem('token');
      const storedUser = localStorage.getItem('user');

      if (storedToken && storedUser) {
        const parsedUser: User = JSON.parse(storedUser);
        // Basic validation
        if (parsedUser && parsedUser.id && parsedUser.username) {
          setToken(storedToken);
          setUser(parsedUser);
        } else {
          // Invalid data, clear storage
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        }
      } else {
        // If one exists but not the other, clear both for consistency
        if (storedToken || storedUser) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
      }
    } catch (error) {
      console.error('Error reading auth state from localStorage:', error);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    } finally {
      setLoading(false);
    }
  }, []); // Run only once on mount

  // Login function
  const login = useCallback(async (credentials: any) => {
    setLoading(true);
    try {
      const response = await authService.login(credentials);
      const receivedToken = response?.accessToken; // Adjust based on actual response structure
      const receivedUser = response; // Assuming response is the user object

      if (receivedToken && receivedUser && receivedUser.id && receivedUser.username) {
         // Construct user object explicitly
         const userToStore: User = {
            id: receivedUser.id,
            username: receivedUser.username,
            email: receivedUser.email,
            roles: receivedUser.roles,
            // Add other necessary fields from response
         };

        // Update state
        setToken(receivedToken);
        setUser(userToStore);

        // Update localStorage (centralized here)
        localStorage.setItem('token', receivedToken);
        localStorage.setItem('user', JSON.stringify(userToStore));

      } else {
        // Handle cases where login succeeds but token/user data is missing
        console.error('Login successful but token or user data missing in response.');
        // Clear any potentially inconsistent state/storage
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
        throw new Error('Login failed: Incomplete data received.');
      }
    } catch (error) {
      // Clear state and storage on login failure
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      setToken(null);
      setUser(null);
      console.error('Login failed:', error);
      throw error; // Re-throw error to be caught by the calling component
    } finally {
      setLoading(false);
    }
  }, []); // No dependencies needed as authService is stable

  // Logout function
  const logout = useCallback(() => {
    setLoading(true); // Indicate activity
    try {
      authService.logout(); // This handles localStorage clearing and redirect
      // Clear state immediately
      setUser(null);
      setToken(null);
    } catch (error) {
        console.error("Error during logout:", error);
    } finally {
        // setLoading(false); // No need to set loading false as page will redirect
    }
    // Note: authService.logout() performs the redirect, so state updates
    // here are mainly for immediate UI feedback before navigation.
  }, []); // No dependencies needed

  const value = { user, token, loading, login, logout };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// Custom hook to use the AuthContext
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
