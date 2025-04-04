'use client'

import { AppBar, Toolbar, Typography } from '@mui/material';
import Link from 'next/link'
import Image from 'next/image'
import { useState, useEffect } from 'react'
import { authService } from '../services/auth.service'

interface HeaderProps {
  title: string;
}

export default function Header({ title }: HeaderProps) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false) // Add state for mobile menu

  useEffect(() => {
    setIsAuthenticated(authService.isAuthenticated())

    // Optional: Add event listener for logout to update header
    const logoutListener = () => setIsAuthenticated(false);
    window.addEventListener('logout', logoutListener);

    return () => {
      window.removeEventListener('logout', logoutListener)
    }
  }, [])

  return (
    <AppBar position="static">
      <Toolbar className="justify-between">
        <Typography variant="h6">{title}</Typography>
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
          <Link
            href="/about-us"
            className="text-white hover:text-blue-200 transition-colors"
          >
            About Us
          </Link>
          <Link
            href="/contact"
            className="text-white hover:text-blue-200 transition-colors"
          >
            Contact
          </Link>
          {!isAuthenticated && (
            <Link
              href="/auth/login"
              className="text-white hover:text-blue-200 transition-colors"
            >
              Sign In
            </Link>
          )}
        </nav>

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
          <Link
            href="/about-us"
            className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
          >
            About Us
          </Link>
          <Link
            href="/contact"
            className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
          >
            Contact
          </Link>
          {!isAuthenticated && (
            <Link
              href="/auth/login"
              className="block px-3 py-2 rounded-md text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-200"
            >
              Sign In
            </Link>
          )}
        </div>
      </div>
    </AppBar>
  )
}
