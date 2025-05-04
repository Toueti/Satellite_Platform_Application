'use client'

import { AppBar, Toolbar, Typography, Box } from '@mui/material'; // Added Box
import Link from 'next/link'
import Image from 'next/image'
import { useState, useEffect } from 'react'
import { authService } from '../services/auth.service'
import { useAuth } from '../hooks/useAuth'; // Import useAuth
import AccountCircle from '@mui/icons-material/AccountCircle'; // Import AccountCircle icon

interface HeaderProps {
  title: string;
}

export default function Header({ title }: HeaderProps) {
  // Use useAuth hook to get user details when available
  const { user, loading: authLoading } = useAuth();
  // Local state for immediate auth check and polling updates
  // Initialize isLoggedIn based on client-side check after mount
  const [isLoggedIn, setIsLoggedIn] = useState(false); // Default to false initially
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [hasMounted, setHasMounted] = useState(false); // State to track client mount

  useEffect(() => {
    // Set mounted state on client
    setHasMounted(true);

    // Perform initial auth check only on the client after mount
    setIsLoggedIn(authService.isAuthenticated());

    let intervalId: NodeJS.Timeout | null = null;

    // Function to check auth status
    const checkAuth = () => {
      const currentAuthStatus = authService.isAuthenticated();
      setIsLoggedIn(currentAuthStatus);
      if (currentAuthStatus && intervalId) {
        // If authenticated, stop polling
        clearInterval(intervalId);
        intervalId = null;
      }
    };

    // Initial check
    checkAuth();

    // Start polling only if not logged in initially
    if (!isLoggedIn) {
      intervalId = setInterval(checkAuth, 5000); // Poll every 5 seconds
    }

    // Cleanup interval on component unmount
    return () => {
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, [isLoggedIn]); // Re-run effect if isLoggedIn changes (to stop polling)

  // Removed conflicting useEffect block that was commented out previously

  return (
    <AppBar position="static">
      <Toolbar className="justify-between">
        {/* Logo and App Name */}
        <div className="flex items-center">
          <Link href="/" className="flex items-center space-x-2">
            <div className="relative w-10 h-10">
              <Image
                src="/images/logo.png"
                alt="SatelliteIP Logo"
                fill
                sizes="(max-width: 640px) 40px, 40px"
                priority
                className="object-contain"
              />
            </div>
            <span className="text-xl font-bold text-white">SatelliteIP</span>
          </Link>
        </div>

        {/* Removed the Typography element that displayed the title prop */}
        {/* <Typography variant="h6">{title}</Typography> */}

        {/* Navigation items pushed to the right */}
        <nav className="hidden md:flex items-center space-x-8 ml-auto">
          <Link
            href="/"
            className="text-white hover:text-blue-200 transition-colors"
          >
            Home
          </Link>
          <Link
            href="/dashboard"
            className="text-white hover:text-blue-200 transition-colors"
          >
            Dashboard
          </Link>
          {/* Auth status - Render only after client mount to prevent hydration errors */}
          {hasMounted && (
            isLoggedIn ? (
              // User is logged in, display generic status
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AccountCircle sx={{ color: 'white' }} />
                <Typography variant="body1" sx={{ color: 'white' }}>
                  Logged In
                </Typography>
              </Box>
            ) : ( // This is the ':' for the outer ternary (isLoggedIn ? ... : ...)
            // User is not logged in
            <Link
              href="/auth/login"
              className="text-white hover:text-blue-200 transition-colors"
            // Removed the erroneous extra colon and misplaced Link block below
            // ) : (
            //   // User is not logged in
            //   <Link
            //     href="/auth/login"
            //     className="text-white hover:text-blue-200 transition-colors"
            >
              Sign In
            </Link>
            )
          )}
          {/* Placeholder during SSR / before mount */}
          {!hasMounted && <span className="text-white opacity-0">Sign In</span>}
        </nav>
        {/* Removed duplicated nav content here */}

        {/* Mobile menu button */}
        <div className="md:hidden">
          <button
            type="button"
            aria-label="Toggle navigation menu"
            aria-expanded={isMobileMenuOpen}
            className="text-white hover:text-blue-200 focus:outline-none focus:ring-2 focus:ring-blue-200 focus:ring-offset-2 rounded-md p-2"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            <svg
              className="h-6 w-6"
              fill="none"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              viewBox="0 0 24 24"
              stroke="currentColor"
              aria-hidden="true"
            >
              <path d="M4 6h16M4 12h16M4 18h16"></path>
            </svg>
          </button>
        </div>
      </Toolbar>

      {/* Mobile menu */}
      <div
        className={`${
          isMobileMenuOpen ? 'block' : 'hidden'
        } md:hidden absolute top-full left-0 w-full bg-gray-800 shadow-lg`}
        id="mobile-menu"
      >
        <div className="px-2 pt-2 pb-3 space-y-1">
          <Link
            href="/"
            className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
          >
            Home
          </Link>
          <Link
            href="/dashboard"
            className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
          >
            Dashboard
          </Link>
          {/* Conditional rendering for mobile menu - Render only after client mount */}
          {hasMounted && (
            isLoggedIn ? (
              // User is logged in, display generic status in mobile menu
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 3, py: 2 }}>
                <AccountCircle sx={{ color: 'white' }} />
                <Typography variant="body1" sx={{ color: 'white' }}>
                  Logged In
                </Typography>
              </Box>
            ) : ( // This is the ':' for the outer ternary (isLoggedIn ? ... : ...)
            // User is not logged in
            <Link
              href="/auth/login"
              className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
            // Removed the erroneous extra colon and misplaced Link block below
            // ) : (
            //   <Link
            //     href="/auth/login"
            //     className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
            >
              Sign In
            </Link>
            )
          )}
          {/* Placeholder during SSR / before mount */}
          {!hasMounted && <span className="block px-3 py-2 rounded-md text-white opacity-0">Sign In</span>}
        </div>
      </div>
      {/* Removed duplicated mobile menu content here */}
    </AppBar>
  ); // Ensure single return and correct closing
}
